import os

from ArgumentParser import ArgumentParser

transformed = '/transformed/'
preprocessed = '/preprocessed/'


def get_line_count(filepath):
    with open(filepath) as f:
        lines = f.readlines()
    return len(lines)


def contains_all_input_folders(path):
    expected_folders = ['preprocessed', 'transformed', 'consensus']
    contents = os.listdir(path)
    return list(set(expected_folders) - set(contents))


def get_line_counts_of_output_files(folder):
    preprocessed_folder = folder + preprocessed
    transformed_folder = folder + transformed
    preprocessed_contents = os.listdir(preprocessed_folder)
    line_counts = {}
    for filename in preprocessed_contents:
        transformed_file_name = 'vkgl_{}.tsv'.format(filename.split('.')[0])
        transformed_error_file_name = 'vkgl_{}_error.txt'.format(filename.split('.')[0])
        line_counts[filename] = {
            preprocessed + filename: get_line_count(preprocessed_folder + filename),
            transformed + filename: get_line_count(transformed_folder + filename),
            transformed + transformed_file_name: get_line_count(transformed_folder + transformed_file_name),
            transformed + transformed_error_file_name: get_line_count(
                transformed_folder + transformed_error_file_name)
        }
    return line_counts


def compare_line_counts(line_counts, error_types):
    checks = {'errors': 0, 'passed': 0}
    for filename in line_counts:
        enriched_file_name = transformed + filename
        transformed_file_name = '{}vkgl_{}.tsv'.format(transformed, filename.split('.')[0])
        error_file_name = 'vkgl_{}_error.txt'.format(filename.split('.')[0])
        enriched_file_length = line_counts[filename][enriched_file_name]
        transformed_file_length = line_counts[filename][transformed_file_name]
        # check diff between preprocessed and transformed + errors
        errors_plus_transformed_lines = line_counts[filename][transformed_file_name] + error_types[error_file_name][
            'other']
        preprocessed_lines = line_counts[filename][preprocessed + filename]
        errors_plus_transformed_lines_dups = errors_plus_transformed_lines + error_types[error_file_name]['dup']
        errors = 0
        if enriched_file_length != transformed_file_length:
            print('❌ Length of [{}]({}) not equal to length of [{}]({})'.format(enriched_file_name,
                                                                                enriched_file_length,
                                                                                transformed_file_name,
                                                                                transformed_file_length
                                                                                ))
            errors += 1
            checks['errors'] += 1

        # radboud preprocessed file misses the header
        if ('radboud' not in filename and preprocessed_lines != errors_plus_transformed_lines_dups) or (
                'radboud' in filename and preprocessed_lines + 1 != errors_plus_transformed_lines_dups):
            print('❌ Length of [{}]({}) not equal to length of [{}]({})'.format(preprocessed + filename,
                                                                                preprocessed_lines,
                                                                                '{} + {}'.format(
                                                                                    transformed_file_name,
                                                                                    error_file_name),
                                                                                    errors_plus_transformed_lines_dups))
            errors += 1
            checks['errors'] += 1

        if errors == 0:
            print('✅ {} (2 checks passed)'.format(filename))
            checks['passed'] += 2
    return checks


def get_error_types(transformed_folder):
    errors = {}
    error_postfix = '_error.txt'
    for filename in os.listdir(transformed_folder):
        if filename.endswith(error_postfix):
            with open(transformed_folder + filename) as errorfile:
                errors[filename] = {
                    'dup': 0,
                    'other': 0
                }
                for line in errorfile.readlines():
                    columns = line.split('\t')
                    reason = columns[-1]
                    if 'Variant duplicated' in reason:
                        errors[filename]['dup'] += len(reason.split(','))
                    else:
                        errors[filename]['other'] += 1
    return errors


def main():
    folder = ArgumentParser().folder
    # Check if all output folders are there
    missing_folders = contains_all_input_folders(folder)
    errors = 0
    passed = 0
    if len(missing_folders) == 0:
        passed += 1
        print('✅ Preprocessed, transformed and consensus folders present')
    else:
        errors += 1
        print('❌ [{}] folder(s) missing from selected output folder'.format(','.join(missing_folders)))

    if 'transformed' not in missing_folders:
        # Check if linecount in processed data are the same in enriched and output data
        line_counts = get_line_counts_of_output_files(folder)
        error_types = get_error_types(folder + transformed)
        line_checks = compare_line_counts(line_counts, error_types)

        if errors > 0 or line_checks['errors'] > 0:
            print('\n💥 FAILED: {} out of {} checks failed'.format(errors + line_checks['errors'],
                                                                   passed + errors + line_checks['passed'] +
                                                                   line_checks['errors']))
        else:
            print('\n🎉 SUCCESS: {} out of {} checks passed'.format(passed + line_checks['passed'],
                                                                    passed + line_checks['passed']))


if __name__ == '__main__':
    main()
