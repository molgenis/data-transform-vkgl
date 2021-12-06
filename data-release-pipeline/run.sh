#!/bin/bash

# Initialize SCRIPT_DIR
if [[ -n "${SLURM_JOB_ID}" ]]; then SCRIPT_DIR=$(dirname "$(scontrol show job "${SLURM_JOB_ID}" | awk -F= '/Command=/{print $2}' | cut -d ' ' -f 1)"); else SCRIPT_DIR=$(dirname "$(realpath "$0")"); fi
SCRIPT_NAME="$(basename "$0")"

set -euo pipefail

usage() {
  echo -e "usage: ${SCRIPT_NAME} -l <arg> -r <arg>

-r, --release   <arg>  required: name of the directory for this release in each home dir on the ftp server.
-s, --server    <arg>  required: name of the ftp server in your ssh config.

--hgnc_biomart_genes <arg>  optional: path to HGNC BioMart gene file (default: download latest dataset)."
}

# arguments:
#   $1 path to input directory
#   $2 path to output directory
generateConsensus() {
  local -r inputDir="${1}"
  local -r outputDir="${2}"

  mkdir -p "${outputDir}"

  wget -q -c https://github.com/molgenis/vkgl-consensus/releases/download/v1.0.0/vkgl-consensus.jar

  module load Java

  local args=()
  args+=("-jar" "vkgl-consensus.jar")
  args+=("--amc" "${inputDir}/vkgl_vkgl_export_amc_20210614.tsv")
  args+=("--erasmus_mc" "${inputDir}/vkgl_vkgl_export_erasmusmc_20210614.tsv")
  args+=("--lumc" "${inputDir}/vkgl_lumc.tsv")
  args+=("--nki" "${inputDir}/vkgl_vkgl_export_nki_20210614.tsv")
  args+=("--radboud_mumc" "${inputDir}/vkgl_radboud_mumc.tsv")
  args+=("--umc_utrecht" "${inputDir}/vkgl_vkgl_export_umcutrecht_20210614.tsv")
  args+=("--vumc" "${inputDir}/vkgl_vkgl_export_vumc_20210614.tsv")
  args+=("--umcg" "${inputDir}/vkgl_vkgl_export_umcg_20210614.tsv")
  args+=("-o" "${outputDir}/consensus.tsv")

  java "${args[@]}"

  module purge
}

# arguments:
#   $1 path to input directory
#   $2 path to HGNC BioMart gene file
#   $3 path to output directory
transformData() {
  local -r inputDir="${1}"
  local -r hgncBiomartGenesFilePath="${2}"
  local -r outputDir="${3}"

  mkdir -p "${outputDir}"

  dataTransformVersion="2.0.5"

  wget -q -c https://github.com/molgenis/data-transform-vkgl/archive/refs/tags/${dataTransformVersion}.tar.gz -O - | tar -xz -C "${outputDir}"
  wget -q -c https://mirror.lyrahosting.com/apache/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz -O - | tar -xz -C "${outputDir}"

  local -r dataTransformVkglDir="${outputDir}/data-transform-vkgl-${dataTransformVersion}"
  local -r inboxDir="${dataTransformVkglDir}/src/test/inbox"
  mkdir -p "${inboxDir}"
  cp "${inputDir}"/* "${inboxDir}"

  shopt -s nullglob
  local -r numFilesArr=("${inputDir}"/*)
  local -r numFiles=${#numFilesArr[@]}

  module load Java
  "${outputDir}/apache-maven-3.8.1/bin/mvn" -f "${dataTransformVkglDir}/pom.xml" clean spring-boot:run -Dspring-boot.run.arguments="--server.port=0 --hgnc.genes=${hgncBiomartGenesFilePath}" >"${outputDir}/transform.log" 2>&1 &
  local -r javaPid=$!

  local numProcessedFiles="0"
  local newNumProcessedFiles=""
  while :; do
    set +e
    if [[ "$(grep -c " ERROR " "${outputDir}/transform.log")" -gt 0 ]]; then
      echo "error occurred processing files, see ${outputDir}/transform.log"
      break
    fi
    newNumProcessedFiles="$(grep -cE "\[.+ - Delay\] done" "${outputDir}/transform.log")"
    set -e
    if [[ "${newNumProcessedFiles}" -gt "${numProcessedFiles}" ]]; then
      echo "processed ${newNumProcessedFiles}/${numFiles} files ..."
    fi
    numProcessedFiles="${newNumProcessedFiles}"

    if [[ "${numProcessedFiles}" == "${numFiles}" ]]; then
      break
    fi
    sleep 10
  done

  kill "${javaPid}"
  module purge

  if [[ "${numProcessedFiles}" < "${numFiles}" ]]; then
    exit 1
  fi

  local -r outboxDir="${dataTransformVkglDir}/result"
  mv "${outboxDir}"/* "${outputDir}"
}

# arguments:
#   $1 path to output directory
#   $2 path to folder containing downloaded variant files
preprocessData() {
  local -r outputDir="${1}"
  local -r inputDir="${2}"

  mkdir -p "${outputDir}"

  cp "${lumcFilePath}" "${outputDir}"
  cp "${radboudMumcFilePath}" "${outputDir}"

  # prepend empty timestamp and id column to S3 data
  local outputFilePath=""
  for entry in "${s3Dir}"/*; do
    [[ -f "${entry}" && "${entry}" != *.log ]] || continue
    outputFilePath="${outputDir}/$(basename "${entry}")"
    head -n 1 "${entry}" | awk '{print "timestamp\tid\t"$0}' >"${outputFilePath}"
    tail -n +2 "${entry}" | awk '{print "\t\t"$0}' >>"${outputFilePath}"
  done

  # merge VUMC data
  local vumc=""
  local vumcWes=""
  for entry in "${outputDir}"/*; do
    if [[ $(basename "${entry}") =~ vkgl_export_vumc_[[:digit:]]{8}\.tsv ]]; then
      vumc="${entry}"
    elif [[ $(basename "${entry}") =~ vkgl_export_vumc_wes_[[:digit:]]{8}\.tsv ]]; then
      vumcWes="${entry}"
    fi
  done
  if [[ -z ${vumc} ]]; then
    exit 1
  fi
  if [[ -z ${vumcWes} ]]; then
    exit 1
  fi
  tail -n +2 "${vumcWes}" >>"${vumc}"
  rm "${vumcWes}"

  # dev: only store first 100 lines
  #for entry in "${outputDir}"/*; do
  #  head -n 100 "${entry}" > "${entry}.tmp"
  #  rm "${entry}"
  #  mv "${entry}.tmp" "${entry}"
  #done
}

# arguments:
#   $1 release name
#   $2 server name
#   $3 path of the rsync files list
#   $4 path to output directory
downloadLabData() {
  local -r release="${1}"
  local -r server="${2}"
  local -r filesList="${3}"
  local -r outputDir="${4}"

  mkdir -p "${outputDir}"

  rsync --include-from "${filesList}" --exclude='*' --rsh='ssh -p 443 -l umcg-vkgl-alissa' "${server}"::home/"${release}"/ ${outputDir}
  rsync --include-from "${filesList}" --exclude='*' --rsh='ssh -p 443 -l umcg-vkgl-lumc' "${server}"::home/"${release}"/ ${outputDir}
  rsync --include-from "${filesList}" --exclude='*' --rsh='ssh -p 443 -l umcg-vkgl-radboud' "${server}"::home/"${release}"/ ${outputDir}

  module purge
}

# arguments:
#   $1 path to output file
downloadHgncBiomartGenes() {
  local -r outputFilePath="${1}"

  mkdir -p "$(dirname "${outputFilePath}")"

  curl 'https://biomart.genenames.org/martservice/results' \
    -s \
    --compressed \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data 'download=true&query=%3C%21DOCTYPE+Query%3E%3CQuery+client%3D%22biomartclient%22+processor%3D%22TSV%22+limit%3D%22-1%22+header%3D%221%22%3E%3CDataset+name%3D%22hgnc_gene_mart%22+config%3D%22hgnc_gene_config%22%3E%3CAttribute+name%3D%22hgnc_gene__hgnc_gene_id_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__status_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__approved_symbol_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__approved_name_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__hgnc_alias_symbol__alias_symbol_108%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__hgnc_previous_symbol__previous_symbol_1012%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__chromosome_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__chromosome_location_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__locus_group_1010%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__ncbi_gene__gene_id_1026%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__ensembl_gene__ensembl_gene_id_104%22%2F%3E%3CAttribute+name%3D%22hgnc_gene__ucsc__ucsc_gene_id_1035%22%2F%3E%3C%2FDataset%3E%3C%2FQuery%3E' \
    -o "${outputFilePath}"
}

# arguments:
#   $1 Release name (name of the folder on the ftp server)
#   $2 name of the ftp server as stated in your ssh config
#   $3 path to the files list for rsync
#   $4 path to HGNC BioMart gene file (optional)
# returns:
#    1 if input is invalid
validate() {
  local -r release="${1}"
  local -r server="${2}"
  local -r filesList="${3}"
  local -r hgncBiomartGenesFilePath="${3}"

  if [[ -z "${release}" ]]; then
    echo -e "missing required argument for option -r / --release."
    return 1
  fi

  if [[ -z "${server}" ]]; then
    echo -e "missing required argument for option -s / --server."
    return 1
  fi

  if [[ -z "${filesList}" ]]; then
    echo -e "missing required argument for option -l / --files-list."
    return 1
  fi
  if [[ ! -f "${filesList}" ]]; then
    echo -e "input ${filesList} for option -l / --files-list does not exist."
    return 1
  fi

  if [[ -n "${hgncBiomartGenesFilePath}" ]] && [[ ! -f "${hgncBiomartGenesFilePath}" ]]; then
    echo -e "input ${hgncBiomartGenesFilePath} for option --hgnc_biomart_genes does not exist."
    return 1
  fi
}

main() {
  local -r parsedArgs=$(getopt -a -n pipeline -o l:r:s: --long lumc:,radboud_mumc:,s3:,aws_credentials:,aws_config:,hgnc_biomart_genes: -- "$@")

  local release=""
  local server=""
  local hgncBiomartGenesFilePath=""

  eval set -- "${parsedArgs}"
  while :; do
    case "$1" in
    -r | --release)
      release="$2"
      shift 2
      ;;
    -l | --files-list)
      filesList=$(realpath "$2")
      shift 2
      ;;
    -s | --server)
      server="$2"
      shift 2
      ;;
    --hgnc_biomart_genes)
      hgncBiomartGenesFilePath=$(realpath "$2")
      shift 2
      ;;
    --)
      shift
      break
      ;;
    *)
      usage
      exit 2
      ;;
    esac
  done

  if ! validate "${release}" "${server}" "${filesList}" "${hgncBiomartGenesFilePath}"; then
    usage
    exit 1
  fi

  echo "running ..."

  local -r outputDir="${SCRIPT_DIR}/$(date +"%Y%m%d%H%M")"
  mkdir "${outputDir}"

  echo "writing output to ${outputDir}"

  if [[ -z "${hgncBiomartGenesFilePath}" ]]; then
    echo "downloading HGNC BioMart genes ..."
    hgncBiomartGenesFilePath="${outputDir}/downloads/hgnc_genes_$(date +"%Y%m%d").tsv"
    downloadHgncBiomartGenes "${hgncBiomartGenesFilePath}"
    echo "downloading HGNC BioMart genes done"
  fi

  echo "downloading lab data ..."
  downloadLabData "${outputDir}" "${server}" "${filesList}" "${release}"
  echo "downloading lab data done"

  echo "preprocessing data ..."
  local -r preprocessedDataDir="${outputDir}/preprocessed"
  preprocessData "${preprocessedDataDir}" "${outputDir}"
  echo "preprocessing data done"

  echo "transforming data ..."
  local -r transformedDataDir="${outputDir}/transformed"
  transformData "${preprocessedDataDir}" "${hgncBiomartGenesFilePath}" "${transformedDataDir}"
  echo "transforming data done"

  echo "generating consensus ..."
  local -r consensusDataDir="${outputDir}/consensus"
  generateConsensus "${transformedDataDir}" "${consensusDataDir}"
  echo "generating consensus done"

  echo "running done"
}

main "${@}"
