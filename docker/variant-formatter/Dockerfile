FROM python:2.7

RUN apt-get update
RUN apt-get -yq install git sqlite rsync

RUN pip install pandas 
RUN pip install bioutils==0.4.3 
RUN pip install hgvs
RUN pip install web.py

COPY hgvs2vcf.py .
COPY Babelfish.py .
COPY server.py .

CMD sleep 5s && seqrepo list-local-instances && python ./server.py 1234