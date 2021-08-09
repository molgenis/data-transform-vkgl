import argparse


class ArgumentParser:
    def __init__(self):
        self.folder = ''
        self.parse()

    def parse(self):
        parser = argparse.ArgumentParser(description="Validate results of VKGL script")
        parser.add_argument("folder", type=str, help="Folder with output of VKGL script")
        args = parser.parse_args()
        self.folder = args.folder
