#!/usr/local/bin/perl -w
# Given one or more GAME XML files, run them through Apollo (so it loads and
# then saves them) and then run Chris's XML differ to make sure nothing important
# changed during the roundtrip.

my $DEBUG = 0;
my $APOLLO_BIN = $0;
$APOLLO_BIN =~ s/\/[^\/]+$//;
print "APOLLO_BIN = $APOLLO_BIN\n" if $DEBUG;

if ($#ARGV < 0) {
    die("Usage: $0 file1.xml [file2.xml etc]\n");
}

foreach my $file (@ARGV) {
    chomp($file);
    $saved = "$file.saved";
    if (!($saved =~ /^\//)) {
	my $pwd = `pwd`;
	chomp($pwd);
	$saved = "$pwd/$saved";
    }
    my $cmd = "$APOLLO_BIN/apollo -test -x $file -x $saved 2>&1";
    print "$cmd\n";
    @rt = `$cmd`;
    if ($DEBUG) {
	foreach my $line (@rt) {
	    print $line;
	}
    }

    if (!-s("$saved")) {
	print "ERROR: couldn't round-trip $file\n";
	foreach my $line (@rt) {
	    print $line;
	}
	next;
    }
    $cmd = "$APOLLO_BIN/stag-diff.pl $file $saved";
    print "$cmd\n";
    my @diff = `$cmd`;
    foreach my $line (@diff) {
	print $line;
    }
}
