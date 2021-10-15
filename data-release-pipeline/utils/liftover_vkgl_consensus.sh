#!/bin/bash

# uses Picard and BCFtools easybuild modules
# tsv-converter-jar (https://github.com/molgenis/tsv-vcf-converter) must be available in the same folder as this script.

INPUT=$1
OUTPUT=$2
FORCE=false

if [[ -z "${INPUT}" ]]; then
	echo "usage:"
	echo "bash ./liftover_vkgl_consensus.sh /path/to/input/file.tsv /path/to/output/file.tsv [add -f to overwrite exising files.]"
	exit 1
fi

if [[ -n "${3}" ]]
  then
  if [[ "${3}" == "--force" ]] || [[ "${3}" == "-f" ]]; then
	FORCE=true
  fi
fi

if [ -d "./liftover_tmp" ];	then
		if [ $FORCE == true ]
		then
			rm -R ./liftover_tmp
		else
			echo "Directory ./liftover_tmp exists, use -f or --force to overwrite!"
			exit 1
		fi
else
    mkdir ./liftover_tmp
fi

if [ ! -f "${INPUT}" ]; then
    echo "File '${INPUT}' not found!"
	exit 1
fi

if [ -f "${OUTPUT}" ] && [ $FORCE == false ]; then
	echo "File '${OUTPUT}' already exists, use -f or --force to overwrite!"
	exit 1
fi

if [ ! -f ./tsv-vcf-converter.jar ]; then
    echo "File './tsv-vcf-converter.jar' not found!"
	exit 1
fi

ml picard
ml BCFtools

java -jar ./tsv-vcf-converter.jar -i "${INPUT}" -m "CHROM=chromosome,POS=start,STOP=stop,REF=ref,ALT=alt" -o ./liftover_tmp/consensus_b37.vcf -f
java -jar ${EBROOTPICARD}/picard.jar LiftoverVcf I=./liftover_tmp/consensus_b37.vcf O=./liftover_tmp/consensus_b38.vcf CHAIN=/apps/data/GRC/b37ToHg38.over.chain REJECT=./${OUTPUT}_rejected.vcf R=/apps/data/GRC/GCA_000001405.15/GCA_000001405.15_GRCh38_full_plus_hs38d1_analysis_set.fna.gz
bcftools norm -c w -f /apps/data/GRC/GCA_000001405.15/GCA_000001405.15_GRCh38_full_plus_hs38d1_analysis_set.fna.gz -m -both -s -o ./liftover_tmp/consensus_b38_normalized.vcf.gz -O z ./liftover_tmp/consensus_b38.vcf
java -jar ./tsv-vcf-converter.jar -i ./liftover_tmp/consensus_b38_normalized.vcf.gz -m "CHROM=chromosome,POS=start,STOP=stop,REF=ref,ALT=alt" -o "${OUTPUT}" -f

ml purge

rm -R ./liftover_tmp

echo "Finished consensus liftover. Liftovered result: ${OUTPUT}"
echo "Variants that failed to be liftovered: ${OUTPUT}_rejected.vcf"