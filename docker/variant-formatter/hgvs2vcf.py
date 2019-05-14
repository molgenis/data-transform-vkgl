#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import absolute_import, division, print_function, unicode_literals

__doc__ = """add HGVS tags to a VCF file on stdin, output to stdout
eg$ vcf-add-hgvs <in.vcf >out.vcf
"""

import gzip
import itertools
import logging
import os
import sys
import pandas
import hgvs.dataproviders.uta

from Babelfish import Babelfish
from hgvs.parser import Parser
from hgvs.exceptions import HGVSParseError

_logger = logging.getLogger(__name__)

parser = Parser()

def h2v(babelfish, gdna, keep_left_anchor=True):
    try:
        variant = parser.parse_hgvs_variant(gdna)
        (chrom, pos, ref, alt, typ) = babelfish.hgvs_to_vcf(variant)

        if not(keep_left_anchor):
            pfx = os.path.commonprefix([ref, alt])
            lp = len(pfx)
            if lp > 0:
                ref = ref[lp:]
                alt = alt[lp:]
                pos += lp
                if ref == '':
                    ref = '.'
                if alt == '':
                    alt = '.'

        return {'chrom': chrom, 'pos': pos, 'ref': ref, 'alt': alt, 'type': typ}
    except HGVSParseError as e:
        return {'error': e.message}

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    hdp = hgvs.dataproviders.uta.connect()
    babelfish37 = Babelfish(hdp, assembly_name="GRCh37")

    df = pandas.read_csv('lumc-head.tsv', sep='\t')
    mapped = df['gDNA_normalized'].map(lambda gdna: h2v(babelfish37, gdna))
    
    df['chrom'] = mapped.map(lambda result: result['chrom'])
    df['pos'] = mapped.map(lambda result: result['pos'])
    df['ref'] = mapped.map(lambda result: result['ref'])
    df['alt'] = mapped.map(lambda result: result['alt'])
    df['type'] = mapped.map(lambda result: result['type'])
    df['error'] = mapped.map(lambda result: result['error'])
    
    print(df.to_csv(index=False))