import os
import zipfile


class ScriptUtils:
    @staticmethod
    def str_list_to_int_list(str_list):
        return [int(val) for val in str_list]

    @staticmethod
    def unzip(path_to_zip_file, directory_to_extract_to):
        with zipfile.ZipFile(path_to_zip_file, 'r') as zip_ref:
            extracted_file_name = zip_ref.namelist()[0]
            zip_ref.extractall(directory_to_extract_to)
            return extracted_file_name

    @staticmethod
    def create_directory_if_not_exists(directory):
        dir_name = directory.split(os.sep)[-1]
        # Check if parent dir exists
        if os.path.isdir(directory.replace(dir_name, '')):
            # Check if directory already exists
            if not os.path.isdir(directory):
                os.mkdir(directory)
        else:
            print('\nERROR: Directory: [{}] does not exist'.format(directory.replace(dir_name, '')))

    @staticmethod
    def grant_permissions_for_directory(directory):
        for filename in os.listdir(directory):
            os.chmod('{}{}{}'.format(directory, os.sep, filename), 0o775)

    @staticmethod
    def add_sep_to_dir(dir):
        if not dir.endswith(os.sep):
            return dir + os.sep
        else:
            return dir
