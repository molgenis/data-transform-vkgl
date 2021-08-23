import argparse
import os


class ArgumentParser:
    def __init__(self):
        self.vkgl_output = ''
        self.labs = ''
        self.previous_history = ''
        self.input_dir = ''
        self.output_dir = ''
        self.previous_consensus = ''
        self.previous_consensus_comments = ''
        self.parse()

    def persist_args(self):
        with open('{}{}_used_arguments.txt'.format(self.vkgl_output, os.sep), 'w') as arguments:
            arguments.write('--vkgl_output {}'.format(self.vkgl_output))
            arguments.write(' --labs {}'.format(self.labs))
            arguments.write(' --previous_history {}'.format(self.previous_history))
            arguments.write(' --input {}'.format(self.input_dir))
            arguments.write(' --output {}'.format(self.output_dir))
            arguments.write(' --previous_consensus {}'.format(self.previous_consensus))
            arguments.write(' --previous_consensus_comments {}'.format(self.previous_consensus_comments))

    def parse(self):
        parser = argparse.ArgumentParser(description="Validate results of VKGL script")
        parser.add_argument('--vkgl_output', '-v', type=str, help="Folder with output of VKGL script")
        parser.add_argument('--labs', '-l', help="comma-separated list of labs")
        parser.add_argument('--previous_history', '-p', help="comma-separated list of previous history prefixes (yymm)")
        parser.add_argument('--input', '-i', help="input folder for molgenis-py-consensus")
        parser.add_argument('--output', '-o', help="output folder for molgenis-py-consensus")
        parser.add_argument('--previous_consensus', '-c',
                            help="download the tsv file with the consensus of the previous export using the EMX downloader")
        parser.add_argument('--previous_consensus_comments', '-cc',
                            help="download the tsv file with the consensus comments of the previous export using the EMX downloader")
        args = parser.parse_args()
        self.vkgl_output = args.vkgl_output
        self.labs = args.labs
        self.previous_history = args.previous_history
        self.input_dir = args.input
        self.output_dir = args.output
        self.previous_consensus = args.previous_consensus
        self.previous_consensus_comments = args.previous_consensus_comments
        self.persist_args()
