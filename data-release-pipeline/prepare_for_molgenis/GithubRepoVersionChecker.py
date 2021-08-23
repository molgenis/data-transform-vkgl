import re

from ScriptUtils import ScriptUtils


class GithubRepoVersionChecker:
    def get_current_version(self, all_versions, repo):
        mayor = 0
        minor = 0
        patch = 0
        for name in all_versions:
            if re.match(rf'{repo}-\d+\.\d+\.\d+\.zip', name):
                version_str = name.replace('molgenis-py-consensus-', '').replace('.zip', '')
                version = [int(val) for val in version_str.split('.')]
                mayor, minor, patch = self.compare_versions(version, [mayor, minor, patch])
        return '.'.join([str(mayor), str(minor), str(patch)])

    @staticmethod
    def compare_versions(version1, version2):
        version1 = ScriptUtils.str_list_to_int_list(version1)
        version2 = ScriptUtils.str_list_to_int_list(version2)
        if version1[0] > version2[0]:
            return version1
        elif version1[0] == version2[0]:
            if version1[1] > version2[1]:
                return version1
            elif version1[1] == version2[1]:
                if version1[2] > version2[2]:
                    return version1
                else:
                    return version2
            else:
                return version2
        else:
            return version2
