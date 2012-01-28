#!/usr/local/bin/perl -w

BEGIN {
    use lib "$ENV{APOLLO_ROOT}/src/perl";
}
use Apollo::CorbaClient::ORB;
use Error qw(:try);
use Data::Dumper;
use Getopt::Long;

my $h = {};
GetOptions($h, "connect|c=s%", "raw|r", "rawseg|rs", "withseq|ws");

print Dumper $h;
my $ext_id = shift @ARGV;
my $session = Apollo::CorbaClient::ORB->get_session($h->{'connect'});

my $annseq;
my @genes;
my @analysis_l;
try {
    $annseq = 
      $session->get_AnnotatedRegion($ext_id);
    print "Got annseq $annseq\n";
#    my $ss = $annseq->get_Seq("BACR01E01-isle2");
#    dd($ss);
    @genes = @{$annseq->get_gene_list};
    printf "Got %d genes \n", scalar(@genes);
#    $annseq->get_analysis_list;
    @analysis_ids = @{$annseq->get_analysis_id_list};
    printf "Got %d analysis ids \n", scalar(@analysis_ids);
}
catch org::apollo::exceptions::ProcessError with {
    my $e = shift;
    my $r = shift;
    print "caught:$e\nreason:$r\n";
    exit;
};

foreach my $gene (@genes) {
    dd($gene);
    try {
	$exon1_res =
	  $annseq->sequence_region_as_string($gene->{transcripts}->[0]->{exons}->[0]->{range}->{range_min}, 
					     $gene->{transcripts}->[0]->{exons}->[0]->{range}->{range_max},
					    );
    }
    catch org::apollo::exceptions::OutOfRange with {
	my $e = shift;
	warn "Caught an Exception:$e\n";
    };
    print "exon res:$exon1_res\n";
}
print "\n----finished genes----\n";
foreach my $an_id (@analysis_ids) {
    try {
	print "TRYING $an_id\n";
	my $analysis = $annseq->get_analysis_by_id($an_id);
	dd($analysis);
	if (!@{$analysis->{results}}) {
	    next;
	}
        if ($h->{'raw'}) {
            my $strs = $annseq->get_raw_results($an_id, $ext_id);
	    print "RAW:\n";
	    dd($strs);
        }        
        if ($h->{'rawseg'}) {
	    print "fetching raw seqment for result_id ($analysis->{results}->[0]->{result_id}\n";
            my $strs = $annseq->get_raw_results_segment($analysis->{results}->[0]->{result_id}, $ext_id);
	    print "RAW SEGMENT:\n";
	    dd($strs);
	    sleep(2);
        }        
	my %seqh;
	if ($h->{withseq}) {
	    foreach my $rset (@{$analysis->{results}}) {
		foreach my $span (@{$rset->{result_spans}}) {
		    if ($span->{range2}) {
			$seqh{$span->{range2}->{sequence_id}} = 1;
		    }
		}
	    }
	}
	foreach my $seqid (keys %seqh) {
	    my $seq = $annseq->get_Seq($seqid);
	    dd($seq);
	}
    }
    catch org::apollo::exceptions::ProcessError with {
	my $e = shift;
	my $r = shift;
	print "caught:$e\nreason:$r\n";
	exit;
    };
}



#--

sub dd {
    require "Data/Dumper.pm";
    my $ob = shift;
    my $d = Data::Dumper->new(['ob', $ob]);
    print $d->Dump;
}
