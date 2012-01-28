#!/usr/local/bin/perl -w

BEGIN {
    use lib "$ENV{APOLLO_ROOT}/src/perl";
}
use Apollo::CorbaClient::ORB;
use Error qw(:try);
use Data::Dumper;
use Getopt::Long;

my $h = {};
GetOptions($h, "connect|c=s%");
my $manager = Apollo::CorbaClient::ORB->get_obj($h->{'connect'});

print $manager->admin_call(join(" ", @ARGV) || "");
print "\n";

