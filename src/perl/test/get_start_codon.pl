#!/usr/local/bin/perl -w

BEGIN {
    use lib "$ENV{APOLLO_ROOT}/src/perl";
}
use Apollo::CorbaClient::ORB;
use Bio::Seq;
use BioModel::Seq;

use Error qw(:try);

use Getopt::Long;

my $h = {};
GetOptions($h, "connect|c=s%");

print Dumper $h;
my $ext_id = shift @ARGV;
my $session = Apollo::CorbaClient::ORB->get_session($h->{'connect'});

print "connected\n";
my $annseq;
my @genes;
my @analysis_ids;
my $res;
try {
    $annseq = 
      $session->get_AnnotatedRegion($ext_id);
    print "Got annseq $annseq\n";
    $res = $annseq->sequence_as_string;
    printf "Got seq (len %d)\n", length($res);
#    my $seq = BioModel::Seq->new;
#    $seq->residues($res);
#    printf "%s\n",
#    $seq->md5checksum;
    print "$res\ngetting genes....\n";
    @genes = @{$annseq->get_gene_list};
    printf "Got %d genes \n", scalar(@genes);
#    $_ = $res;
#    s/(.{50})/$1\n/g;
#    print ">sq\n$_\n";

}
catch org::apollo::exceptions::ProcessError with {
    my $e = shift;
    my $r = shift;
    print "caught:$e\nreason:$r\n";
    exit;
};

foreach my $gene (@genes) {
    foreach my $t (@{$gene->{transcripts}}) {
    
	my $cds = $t->{cds_range};
	my $codon_res;
	# server does not perform revcom
	$codon_res =
	  $annseq->sequence_region_as_string($cds->{range_min},
					     $cds->{range_min}+2);
	my $codon_res2=
	  substr($res, 
		 $cds->{range_min}-1,
		 3);
	if ($cds->{strand} eq 'minus') {
	    my $str = substr($res,
			     $cds->{range_max}-3,
			     3);
	    $str =~ tr/acgtrymkswhbvdnxACGTRYMKSWHBVDNX/tgcayrkmswdvbhnxTGCAYRKMSWDVBHNX/;
	    $codon_res2 = CORE::reverse $str;

	    $codon_res =
	      $annseq->sequence_region_as_string($cds->{range_max}-2,
						 $cds->{range_max});
	    $codon_res =~ tr/acgtrymkswhbvdnxACGTRYMKSWHBVDNX/tgcayrkmswdvbhnxTGCAYRKMSWDVBHNX/;
	    $codon_res = CORE::reverse $codon_res;

	}
	printf
	  "Gene:%s Transcript:%s Start codon: (%d,%d)%s %s == %s (%s/%s)\n",
	  $gene->{ident}->{name},
	  $t->{ident}->{name},
	  $cds->{range_min},
	  $cds->{range_max},
	  $cds->{strand},
	  $codon_res,
	  $codon_res2,
	  translate($codon_res),
	  translate($codon_res2),
	  ;
    }
}
print "\n----finished genes----\n";

#--

sub dd {
    require "Data/Dumper.pm";
    my $ob = shift;
    my $d = Data::Dumper->new(['ob', $ob]);
    print $d->Dump;
}

sub translate {
    my $seq = Bio::Seq->new(-seq=>shift);
    $seq->translate->str;
}
