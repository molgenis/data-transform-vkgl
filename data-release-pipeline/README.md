# VKGL Release Script

To do a VKGL release, checkout the tag of data-release you want to run in:
[Tags](https://github.com/molgenis/data-transform-vkgl/tags).

Run the script by specifying the name of the release folder on the sftp, and
optionally the file containing the file names that should be downloaded (if other than the default provided in "data-files.txt"), like this:

Make sure you have correctly configured key [ssh key forwardin](http://docs.gcc.rug.nl/gearshift/datatransfers/#configure-ssh-agent-forwarding).

```shell
./run.sh --release 202112 --files-list /path/to/files/list
```

For more information use the `--help` option.

## Validation script

This scripts validates the output directory of `run.sh` that transforms the input of the VKGL export
and creates a simple consensus file.

The easiest way to run the script is in a virtual environment. First change your directory to the
validator folder.

```
python3 -m venv env
```

Now the script should be able to run easily using the following command:

```
source env/bin/activate
pip install -e .
python3 main.py /folder-with-run-output
```

The script will show you if all checks passed and if not, what files are incomplete.

## Running on MacOS

The script can be ran on mac, with a few alterations.

1. Install [macports](https://www.macports.org/install.php). Then install the following:

```shell
sudo port install wget
sudo port install getopt
sudo port install coreutils
```

2. Make sure you have python3 and java11 installed.

3. Remove module load and module purge commands from run script

4. If you have python2 running using the `python` command, change the python command in the run
   script to `python3`.
5. Add realpath function right below `#!/bin/bash`:

```shell
realpath() {
[[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}
```

6. Make sure you have aws credentials and config stored somewhere and refer to them when running the
   script.

7. Make sure you have radboud+mumc and lumc data stored somewhere and refer to them when running the
   script.

8. Run the script as described above.
   
