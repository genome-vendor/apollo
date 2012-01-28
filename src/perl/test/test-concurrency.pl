#!/usr/local/bin/perl -w

use lib "$ENV{APOLLO_ROOT}/src/perl";
use Apollo::CorbaClient::ORB;
use Error qw(:try);
use Data::Dumper;

my @params= ();
my %h = ();
while (@ARGV) {
    my $s = shift @ARGV;
    if ($s =~ /^\-/) {
	$s=~ /^\-//;
	$h{$s} = shift @ARGV;
    }
    else {
	push(@params, $s);
    }
}
print Dumper \%h;
my $ext_id = shift @params;
my $session = Apollo::CorbaClient::ORB->new(\%h);

my $annseq;
my @genes;
my @analysis_l;
try {
    $annseq = 
      $session->get_AnnotatedRegion($ext_id);
    print "Got annseq $annseq\n";
    @genes = @{$annseq->get_gene_list};
    printf "Got %d genes \n", scalar(@genes);
    @analysis_ids = @{$annseq->get_analysis_id_list};
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
