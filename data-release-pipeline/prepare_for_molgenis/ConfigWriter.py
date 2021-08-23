class ConfigWriter:
    @staticmethod
    def create_config_file(config_path, config):
        with open(config_path, 'w') as config_file:
            for item in config:
                config_file.write('{}={}\n'.format(item, config[item]))