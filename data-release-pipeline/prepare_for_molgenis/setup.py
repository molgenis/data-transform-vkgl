from setuptools import setup, find_packages

setup(
    name='vkglValidate',
    version='0.1',
    packages=find_packages(),
    install_requires=['requests==2.26.0'],
    python_requires='>=3.7.1',
    url='',
    license='',
    author='mslofstra',
    author_email='m.k.slofstra@umcg.nl',
    description='Validating script for VKGL consensus created by run.sh'
)
