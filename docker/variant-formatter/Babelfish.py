"""translate between HGVS and other formats"""

import bioutils.assemblies

import hgvs.normalizer
import hgvs.assemblymapper

def _as_interbase(posedit):
    if posedit.edit.type == "ins":
        # ins coordinates (only) exclude left position
        start_i = posedit.pos.start.base
        end_i = posedit.pos.end.base - 1
    else:
        start_i = posedit.pos.start.base - 1
        end_i = posedit.pos.end.base
    return (start_i, end_i)


class Babelfish:
    def __init__(self, hdp, assembly_name):
        self.hdp = hdp
        self.hn = hgvs.normalizer.Normalizer(hdp,
                                             cross_boundaries=False,
                                             shuffle_direction=5,
                                             validate=False)
        self.assembly_mapper = hgvs.assemblymapper.AssemblyMapper(hdp, assembly_name=assembly_name)
        self.ac_to_chr_name_map = {
            sr["refseq_ac"]: sr["name"]
            for sr in bioutils.assemblies.get_assembly(assembly_name)["sequences"]}



    def hgvs_to_vcf(self, var):
        """**EXPERIMENTAL**

        converts a single hgvs allele to (chr, pos, ref, alt) using
        the given assembly_name. The chr name uses official chromosome
        name (i.e., without a "chr" prefix).

        Returns None for non-variation (e.g., NC_000006.12:g.49949407=)

        """

        if var.type == "g":
            var = var
        elif var.type == "c":
            var = self.assembly_mapper.c_to_g(var)
        elif var.type == "n":
            var = self.assembly_mapper.n_to_g(var)
        else:
            raise RuntimeError("Expected g. variant, got {var}".format(var=var))

        vleft = self.hn.normalize(var)

        (start_i, end_i) = _as_interbase(vleft.posedit)

        chr = self.ac_to_chr_name_map[vleft.ac]

        typ = vleft.posedit.edit.type

        if typ == "dup":
            start_i -= 1
            alt = self.hdp.seqfetcher.fetch_seq(vleft.ac, start_i, end_i)
            ref = alt[0]
            end_i = start_i
            return (chr, start_i + 1, ref, alt, typ)

        if vleft.posedit.edit.ref == vleft.posedit.edit.alt:
            return None

        alt = vleft.posedit.edit.alt or ""

        if end_i - start_i == 1 and vleft.posedit.length_change == 0:
            # SNVs aren't left anchored
            ref = vleft.posedit.edit.ref

        else:
            # everything else is left-anchored
            start_i -= 1
            ref = self.hdp.seqfetcher.fetch_seq(vleft.ac, start_i, end_i)
            alt = ref[0] + alt

        return (chr, start_i + 1, ref, alt, typ)