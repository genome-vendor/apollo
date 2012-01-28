#!/usr/local/bin/perl -w


use CORBA::ORBit idl => [ $ENV{APOLLO_ROOT} ?  "$ENV{APOLLO_ROOT}/idl/apollo.idl" : 'apollo.idl' ];
use Error qw(:try);

my $ext_id = shift @ARGV;

my @param_list = ();
$ior_file = "apollo_server.ior";
while (@ARGV) {
    my $n = shift @ARGV;
    # hacky fudge until we get nameservice
    if ($n eq "-ior_url"){
	$ior_file = "remote.ior";
	my $url = shift @ARGV;
	my $cmd = "wget -O $ior_file $url";
	print "cmd=$cmd\n";
	print `$cmd`;
    }
    else {
	push(@param_list, {name=>$n, value=>(shift @ARGV)});
    }
}
print "params=@param_list\n";

print STDERR "Got file $ior_file\n";
$orb = CORBA::ORB_init("orbit-local-orb");

open(F,"$ior_file") || die "Could not open $ior_file";
$ior = <F>;
chomp $ior;
close(F);
 
print "IOR=$ior\n";
my $mgr = $orb->string_to_object($ior);

print "MGR=$mgr\n";
my $session = $mgr->initiate_Session([]);
$session->connect(\@param_list);

print "connected\n";
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
