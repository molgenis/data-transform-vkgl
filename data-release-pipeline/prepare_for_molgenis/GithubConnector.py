import json
import os
import sys

import requests


class GithubConnector:
    def get_latest_release(self, repo):
        response = self.get_response('https://api.github.com/repos/molgenis/{}/releases/latest'.format(repo))
        return response

    def get_version_release(self, repo, version):
        # /repos/{owner}/{repo}/releases/tags/{tag}
        response = self.get_response('https://api.github.com/repos/molgenis/{}/releases/tags/{}'.format(repo, version))
        return response

    @staticmethod
    def get_response(url):
        try:
            response = requests.get(url)
        except:
            url_parts = url.split['/']
            print('ERROR: Cannot retrieve [{}] release of [{}]'.format(url_parts[-1], url_parts[5]))
            sys.exit()
        return json.loads(response.text)

    @staticmethod
    def get_version_from_response(json_response):
        return json_response['tag_name']

    @staticmethod
    def download_zip_from_url(url, save_path, repo, chunk_size=128):
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
