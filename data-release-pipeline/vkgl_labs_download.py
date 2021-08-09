#!/usr/bin/python

import datetime
import sys
import boto3
import re


# based on https://github.com/molgenis/molgenis/blob/molgenis-8.7.2/molgenis-amazon-bucket/src/main/java/org/molgenis/amazon/bucket/client/AmazonBucketClientImpl.java#L89
def main(argv):
    output_path = argv[0]

    amc_key = None
    amc_last_modified = None
    erasmusmc_key = None
    erasmusmc_last_modified = None
    nki_key = None
    nki_last_modified = None
    umcg_artefact_key = None
    umcg_artefact_last_modified = None
    umcg_key = None
    umcg_last_modified = None
    umcutrecht_key = None
    umcutrecht_last_modified = None
    vumc_key = None
    vumc_last_modified = None
    vumc_wes_key = None
    vumc_wes_last_modified = None

    client = boto3.client('s3')
    #outputfile = open('/Users/mslofstra/Documents/tmp/vkgl_jul/output.txt', 'w')
    paginator = client.get_paginator('list_objects')
    page_iterator = paginator.paginate(Bucket='com.cartagenia.consortium.vkgl')
    #outputfile.write(page_iterator)
    #outputfile.close()
    for page in page_iterator:
        for item in page['Contents']:
            if re.fullmatch(r"amc/vkgl_export_amc_\d{8}.+", item['Key']):
                if amc_last_modified is None or item['LastModified'] > amc_last_modified:
                    amc_key = item['Key']
                    amc_last_modified = item['LastModified']
            elif re.fullmatch(r"erasmusmc/vkgl_export_erasmusmc_\d{8}.+", item['Key']):
                if erasmusmc_last_modified is None or item['LastModified'] > erasmusmc_last_modified:
                    erasmusmc_key = item['Key']
                    erasmusmc_last_modified = item['LastModified']
            elif re.fullmatch(r"nki/vkgl_export_nki_\d{8}.+", item['Key']):
                if nki_last_modified is None or item['LastModified'] > nki_last_modified:
                    nki_key = item['Key']
                    nki_last_modified = item['LastModified']
            elif re.fullmatch(r"umcg/artefact_totaal_export_\d{8}.+", item['Key']):
                if umcg_artefact_last_modified is None or item['LastModified'] > umcg_artefact_last_modified:
                    umcg_artefact_key = item['Key']
                    umcg_artefact_last_modified = item['LastModified']
            elif re.fullmatch(r"umcg/vkgl_export_umcg_\d{8}.+", item['Key']):
                if umcg_last_modified is None or item['LastModified'] > umcg_last_modified:
                    umcg_key = item['Key']
                    umcg_last_modified = item['LastModified']
            elif re.fullmatch(r"umcutrecht/vkgl_export_umcutrecht_\d{8}.+", item['Key']):
                if umcutrecht_last_modified is None or item['LastModified'] > umcutrecht_last_modified:
                    umcutrecht_key = item['Key']
                    umcutrecht_last_modified = item['LastModified']
            elif re.fullmatch(r"vumc/vkgl_export_vumc_\d{8}.+", item['Key']):
                if vumc_last_modified is None or item['LastModified'] > vumc_last_modified:
                    vumc_key = item['Key']
                    vumc_last_modified = item['LastModified']
            elif re.fullmatch(r"vumc/vkgl_export_vumc_wes_\d{8}.+", item['Key']):
                if vumc_wes_last_modified is None or item['LastModified'] > vumc_wes_last_modified:
                    vumc_wes_key = item['Key']
                    vumc_wes_last_modified = item['LastModified']

    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=amc_key, Filename=output_path + '/' + amc_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=erasmusmc_key, Filename=output_path + '/' + erasmusmc_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=nki_key, Filename=output_path + '/' + nki_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=umcg_artefact_key, Filename=output_path + '/' + umcg_artefact_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=umcg_key, Filename=output_path + '/' + umcg_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=umcutrecht_key, Filename=output_path + '/' + umcutrecht_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=vumc_key, Filename=output_path + '/' + vumc_key.partition('/')[2])
    client.download_file(Bucket='com.cartagenia.consortium.vkgl', Key=vumc_wes_key, Filename=output_path + '/' + vumc_wes_key.partition('/')[2])


if __name__ == "__main__":
    main(sys.argv[1:])
