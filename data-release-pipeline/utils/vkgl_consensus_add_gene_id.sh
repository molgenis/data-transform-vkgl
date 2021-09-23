#!/bin/bash
set -euo pipefail

SCRIPT_NAME="$(basename "$0")"

usage() {
  echo -e "usage: ${SCRIPT_NAME} -i <arg> -o <arg> -g <arg>

-i, --input              <arg>  required: Input VKGL consensus file (.tsv).
-o, --output             <arg>  required: Output VKGL consensus file (.tsv).
-g, --hgnc_biomart_genes <arg>  required: HGNC genes file used to create the VKGL consensus file (.tsv).

-f, --force                     optional: Override the output file if it already exists."

}

# arguments:
#   $1 path to input file
#   $2 path to output file
#   $3 path to genes file
#   $4 force
validate() {
  local -r inputFilePath="${1}"
  local -r outputFilePath="${2}"
  local -r genesFilePath="${3}"
  local -r force="${4}"

  if [[ -z "${inputFilePath}" ]]; then
    echo -e "error: missing required option -i or --input."
    return 1
  fi
  if [[ ! -f "${inputFilePath}" ]]; then
    echo -e "error: input ${inputFilePath} does not exist."
    return 1
  fi

  if [[ -z "${outputFilePath}" ]]; then
    echo -e "error: missing required option -o or --output."
    return 1
  fi
  if [[ "${force}" == "0" ]] && [[ -f "${outputFilePath}" ]]; then
    echo -e "error: output ${outputFilePath} already exists, use -f to overwrite."
    return 1
  fi

  if [[ -z "${genesFilePath}" ]]; then
    echo -e "error: missing required option -g or --hgnc_biomart_genes."
    return 1
  fi
  if [[ ! -f "${genesFilePath}" ]]; then
    echo -e "error: input genes ${genesFilePath} does not exist."
    return 1
  fi
}

# arguments:
#   $1 path to input file
#   $2 path to output file
#   $3 path to genes file
transform() {
  local -r inputFilePath="${1}"
  local -r outputFilePath="${2}"
  local -r genesFilePath="${3}"

  # parse genes header
  local genesGeneIdColIdx=""
  local genesGeneSymbolColIdx=""
  IFS=$'\t' read -r -a genesColHeader < <(head -n 1 "${genesFilePath}")
  for i in "${!genesColHeader[@]}"
  do
          if [ "${genesColHeader[$i]}" == "HGNC ID" ]; then
                  genesGeneIdColIdx=$(("${i}" + 1))
          fi
          if [ "${genesColHeader[$i]}" == "Approved symbol" ]; then
                  genesGeneSymbolColIdx=$(("${i}" + 1))
          fi
  done
  if [[ -z "${genesGeneIdColIdx}" ]]; then
      echo -e "error: genes input ${genesFilePath} doesn't contain column 'HGNC ID'."
      exit 1
  fi
  if [[ -z "${genesGeneSymbolColIdx}" ]]; then
      echo -e "error: genes input ${genesFilePath} doesn't contain column 'Approved symbol'."
      exit 1
  fi

  # parse genes rows
  local geneId=""
  local geneSymbol=""
  declare -A geneMap
  while IFS=$'\t' read -r geneId geneSymbol
  do
      # workaround for https://github.com/molgenis/data-transform-vkgl/issues/52
      geneMap["${geneSymbol,,}"]="${geneId#"HGNC:"}"
  done < <(tail -n +2 "${genesFilePath}" | cut -d$'\t' -f "${genesGeneIdColIdx}","${genesGeneSymbolColIdx}")

  # parse input header
  local inputGeneSymbolColIdx=""
  IFS=$'\t' read -r -a inputColHeader < <(head -n 1 "${inputFilePath}")
  for i in "${!inputColHeader[@]}"
  do
          if [ "${inputColHeader[$i]}" == "gene" ]; then
                  inputGeneSymbolColIdx=$(("${i}" + 1))
                  break
          fi
  done
  if [[ -z "${inputGeneSymbolColIdx}" ]]; then
      echo -e "error: input ${inputFilePath} doesn't contain column 'gene'."
      exit 1
  fi

  # create input gene_id column
  local -r tmpFilePath=$(mktemp)
  echo "gene_id" >> "${tmpFilePath}"
  while IFS=$'\t' read -r geneSymbol
  do
      # workaround for https://github.com/molgenis/data-transform-vkgl/issues/52
      echo "${geneMap["${geneSymbol,,}"]}" >> "${tmpFilePath}"
  done < <(tail -n +2 "${inputFilePath}" | cut -d$'\t' -f "${inputGeneSymbolColIdx}")

  paste -d'\t' "${inputFilePath}" "${tmpFilePath}" > "${outputFilePath}"
  rm "${tmpFilePath}"
}

main() {
  local -r args=$(getopt -a -n pipeline -o i:o:g:fh --long input:,output:,hgnc_biomart_genes:,force,help -- "$@")
  # shellcheck disable=SC2181
  if [[ $? != 0 ]]; then
    usage
    exit 2
  fi

  local vkglConsensusInputPath=""
  local vkglConsensusOutputPath=""
  local hgncBiomartGenesFilePath=""
  local force=0

  eval set -- "${args}"
  while :; do
    case "$1" in
    -i | --input)
      vkglConsensusInputPath=$(realpath "$2")
      shift 2
      ;;
    -o | --output)
      vkglConsensusOutputPath="$2"
      shift 2
      ;;
    -g | --hngc_biomart_genes)
      hgncBiomartGenesFilePath="$2"
      shift 2
      ;;
    -f | --force)
      force=1
      shift
      ;;
    -h | --help)
      usage
      exit 0
      shift
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

  if ! validate "${vkglConsensusInputPath}" "${vkglConsensusOutputPath}" "${hgncBiomartGenesFilePath}" "${force}"; then
    echo -e "Try '${SCRIPT_NAME} --help' for more information."
    exit 1
  fi

  transform "${vkglConsensusInputPath}" "${vkglConsensusOutputPath}" "${hgncBiomartGenesFilePath}"
}

main "${@}"
