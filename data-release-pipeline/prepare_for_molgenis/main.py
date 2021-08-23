import os
import re
import sys
from shutil import copyfile

from ArgumentParser import ArgumentParser
from ConfigWriter import ConfigWriter
from GithubConnector import GithubConnector
from GithubRepoVersionChecker import GithubRepoVersionChecker
from ScriptUtils import ScriptUtils

github = GithubConnector()
version_checker = GithubRepoVersionChecker()
repo = 'molgenis-py-consensus'

done_msg = '✅ Done'


def retrieve_molgenis_py_consensus(vkgl_dir):
    print('Retrieving newest molgenis-py-consensus...', end=' ')
    available_versions = os.listdir(vkgl_dir)
    current_version = version_checker.get_current_version(available_versions, repo)
    json_response = github.get_latest_release(repo)
    latest_version = github.get_latest_version_from_response(json_response)
    latest_zip_file = '{}{}{}-{}.zip'.format(vkgl_dir, os.sep, repo, latest_version)
    if current_version != '.'.join(
            [str(item) for item in
             version_checker.compare_versions(current_version.split('.'), latest_version.split('.'))]):
        github.download_latest_zip_from_response(json_response, vkgl_dir, repo)
    dirname = ScriptUtils.unzip(latest_zip_file, vkgl_dir)
    print(done_msg)
    return dirname


def create_config(args, filename):
    # Create config
    print('Creating config...', end=' ')
    input_dir = ScriptUtils.add_sep_to_dir(args.input_dir)
    output_dir = ScriptUtils.add_sep_to_dir(args.output_dir)
    config = {
        'labs': args.labs,
        'prefix': 'vkgl_',
        'consensus': 'consensus',
        'comments': 'comments',
        'previous': args.previous_history,
        'history': 'consensus_history',
        'input': input_dir,
        'output': output_dir
    }

    ConfigWriter.create_config_file('{}{}{}config/config.txt'.format(args.vkgl_output, os.sep, filename), config)
    print(done_msg)


def create_dirs_molgenis_py_consensus(args):
    print('Creating input and output folder for molgenis-py-consensus...', end=' ')
    ScriptUtils.create_directory_if_not_exists(args.input_dir)
    ScriptUtils.create_directory_if_not_exists(args.output_dir)
    print(done_msg)


def copy_lab_files_to_input(args):
    print('Moving lab files to input folder...', end=' ')
    transformed_dir = '{}{}transformed'.format(args.vkgl_output, os.sep)
    transformed_files = os.listdir(transformed_dir)
    for transformed_file in transformed_files:
        # The files we need are either:
        #   - starting with vkgl_vkgl, excluding the artefact file and errors
        #   - starting with vkgl_radboud, excluding errors
        #   - starting with vkgl_lumc, excluding errors
        if (transformed_file.startswith('vkgl_vkgl_') and not 'artefact' in transformed_file or
            transformed_file.startswith('vkgl_lumc') or
            transformed_file.startswith('vkgl_radboud_mumc')) and \
                not transformed_file.endswith('_error.txt'):
            # Rename file to make sure files will upload to molgenis
            simple_file_name = transformed_file.replace('vkgl_export_', '').replace('erasmusmc', 'erasmus').replace(
                'umcutrecht', 'umcu')
            simple_file_name = re.sub(r'_\d+', '', simple_file_name)
            copyfile('{}{}{}'.format(transformed_dir, os.sep, transformed_file),
                     '{}{}{}'.format(args.input_dir, os.sep, simple_file_name))
    print(done_msg)


def grant_script_permissions(directories):
    print('Granting permissions to run scripts...', end=' ')
    for dir in directories:
        ScriptUtils.grant_permissions_for_directory(dir)
    print(done_msg)


def setup_venv(script_dir):
    print('Setting up virtual environment to run molgenis-py-consensus...')
    os.chdir(script_dir)
    os.system('{} -m venv env'.format(sys.executable))
    os.system('source env/bin/activate')
    os.system('pip install -e .')
    print(done_msg)


def run_preprocess(script_dirname):
    print('Preparing files for molgenis-py-consensus...', end=' ')
    preprocessor = '{}{}PreProcessor.py'.format(script_dirname, os.sep)
    os.system('{} {}'.format(sys.executable, preprocessor))
    print(done_msg)


def run_history(script_dirname, args):
    print('Writing history from previous export...', end=' ')
    previous = args.previous_history.split(',')[-1]
    copyfile(args.previous_consensus,
             '{}{}vkgl_consensus20{}.tsv'.format(args.input_dir, os.sep, previous))
    copyfile(args.previous_consensus_comments,
             '{}{}vkgl_consensus_comments20{}.tsv'.format(args.input_dir, os.sep, previous))
    preprocessor = '{}{}HistoryWriter.py'.format(script_dirname, os.sep)
    os.system('{} {}'.format(sys.executable, preprocessor))
    print(done_msg)


def main():
    args = ArgumentParser()
    vkgl_dir = args.vkgl_output

    if os.path.isdir(vkgl_dir):
        script_dirname = retrieve_molgenis_py_consensus(vkgl_dir)

        # Create config file for molgenis-py-consensus
        create_config(args, script_dirname)

        # Create input and output folder (in output folder of run.sh)
        create_dirs_molgenis_py_consensus(args)

        # Place labfiles to input folder
        copy_lab_files_to_input(args)

        # Grant permissions to run scripts
        script_dir = '{}{}{}'.format(vkgl_dir, os.sep, script_dirname)
        preprocessing_script_dir = '{}preprocessing'.format(script_dir)
        consensus_script_dir = '{}consensus'.format(script_dir)
        grant_script_permissions([preprocessing_script_dir, consensus_script_dir])

        # Setting up virtual environment to run molgenis-py-consensus
        setup_venv(script_dir)

        # Run preprocess script
        run_preprocess(preprocessing_script_dir)

        # Run history script
        run_history(preprocessing_script_dir, args)

    else:
        print('ERROR: Directory: [{}] does not exist'.format(vkgl_dir))


if __name__ == '__main__':
    main()
