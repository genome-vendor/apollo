#!/usr/local/bin/perl -w

# this tests multiple clients writing to the corba adapter 
# at the same time; it should allow the first one but block
# the others

# to just test basic writes ie without the concurrency test,
# just specify one thread by passing in -nt=1 -t=1

BEGIN {
    use lib "$ENV{APOLLO_ROOT}/src/perl";
}
use Apollo::CorbaClient::ORB;
use Error qw(:try);
use Data::Dumper;
use Getopt::Long;

my $h = {};
my $run = $0;
my @orig = @ARGV;
GetOptions($h, "connect|c=s%", "thread|t=s", "sleep|s=s", "nt=s");
my $n_threads = 3 || $h->{'nt'};

my $out = "out";
my $ext_id = shift @ARGV;
if (!$h->{thread}) {
    my $cmd = "";
    foreach my $t (1..$n_threads) {
	$cmd = "sleep $t; $run -t $t ".join(" ", @orig)."| grep RETURN > $out.$t 2>1 &";
	print "CMD=$cmd\n";
	system($cmd);
	print "execd...\n";
    }
    print STDERR "execd them all.....\n";
    my $done = 0;
    my $successes = 0;
    while (!$done) {
	$done = 1;
	foreach my $t (1..$n_threads) {
	    if (-f "out.$t") {
		print "got $t...\n";
		open(F, $out.$t);
		$str = join("",<>);
		if ($str =~ /Successful/) {
		    $successes++;
		}
		close(F);
	    }
	    else {
		print "out.$t not found\n";
		$done = 0;
	    }
	}    
	sleep 3;
    }
    print "successes=$successes\n";
}
else {
    my $t = $h->{'thread'};
    $h->{connect}->{user} = "$ENV{USER} TEST MODE thread $t";
    my $session = 
      Apollo::CorbaClient::ORB->get_session($h->{'connect'});

    my $annseq;
    my @genes;
    my @analysis_l;
    try {
	$annseq = 
	    $session->get_AnnotatedRegion($ext_id);
	print STDERR "$t Got annseq $annseq\n";
	@genes = @{$annseq->get_gene_list};
	printf STDERR "$t Got %d genes \n", scalar(@genes);
	@analysis_ids = @{$annseq->get_analysis_id_list};
	printf STDERR "$t Got %d analyses \n", scalar(@analysis_ids);
	print STDERR "$t sleeping...\n";
	sleep($h->{sleep} || 15);
	print STDERR "$t going to save\n";
	$annseq->save_AnnotatedGenes([], \@genes, []);
	print STDERR "$t saved\n";
    }
    catch org::apollo::exceptions::ProcessError with {
	my $e = shift;
	my $r = shift;
	print Dumper $e;
	print Dumper $r;
	print "caught:$e\nreason:$r\n";
	exit;
    }
    catch org::apollo::exceptions::NeedsUpdate with {
	my $e = shift;
	my $r = shift;
	print "\nRETURN: NeedsUpdate\n";
	print "caught:$e\nreason:$r\n";
	exit;
    }
    catch org::apollo::exceptions::NoSuchEntity with {
	my $e = shift;
	my $r = shift;
	print "\nRETURN: No such entity\n";
	print "caught:$e\nreason:$r\n";
	exit;
    };
    # successful
    print "\nRETURN: Successful\n";
}
