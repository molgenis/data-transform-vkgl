import json
import os
import sys

import requests


class GithubConnector:
    @staticmethod
    def get_latest_release(repo):
        try:
            response = requests.get('https://api.github.com/repos/molgenis/{}/releases/latest'.format(repo))
        except:
            print('ERROR: Cannot retrieve latest [{}] release'.format(repo))
            sys.exit()
        return json.loads(response.text)

    @staticmethod
    def get_latest_version_from_response(json_response):
        return json_response['tag_name']

    def download_zip_from_url(self, url, save_path, repo, chunk_size=128):
        try:
            r = requests.get(url, stream=True)
        except:
            print('ERROR: Download of [{}] failed'.format(repo))
            sys.exit()
        filename = '{}{}{}-{}.zip'.format(save_path, os.sep, repo, url.split(os.sep)[-1])
        with open(filename, 'wb') as fd:
            for chunk in r.iter_content(chunk_size=chunk_size):
                fd.write(chunk)

    def download_latest_zip_from_response(self, json_response, path, repo):
        zip_url = json_response['zipball_url']
        self.download_zip_from_url(zip_url, path + '', repo)
