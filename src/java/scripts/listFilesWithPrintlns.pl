#!/usr/bin/perl

# ------------------------------------------------------------------
# listFilesWithPrintlns.pl
#
# Search through all of the Apollo .java files, looking for print 
# statements that could be replaced by Log4J calls.  Searches for
# the following:
#
# System.err.print(ln)
# System.out.print(ln)
# printStackTrace
#
# Generally speaking it is not necessary to explicitly print stack 
# traces in Log4J because you can either:
#
# 1. Pass a Throwable to any of the log methods
#   (and then configure Log4J to print the stack trace or not, at
#    runtime, not compile time)
# 2. Enable layout.LocationInfo to track method names and line 
#    numbers, at the cost of some speed.
#
# $Revision: 1.2 $ $Date: 2007/01/03 12:04:05 $ $Author: jcrabtree $
# ------------------------------------------------------------------

use strict;
use FileHandle;

# ------------------------------------------------------------------
# Globals
# ------------------------------------------------------------------

# line-based regexes to look for in the .java files
my @REGEXES = (
	       'System\.err\.print',
	       'System\.out\.print',
	       'printStackTrace',
	       'static\s+.*void\s+.*main\s*\(',
	       'boolean debug',
	       'boolean DEBUG',
	       );

# ------------------------------------------------------------------
# Input
# ------------------------------------------------------------------

my $apolloRoot = $ENV{'APOLLO_ROOT'};

if (!defined($apolloRoot)) {
    die "APOLLO_ROOT not defined";
}
if (!-e $apolloRoot || !-d $apolloRoot) {
    die "$apolloRoot (APOLLO_ROOT) does not exist or is not a directory";
}

# ------------------------------------------------------------------
# Main program
# ------------------------------------------------------------------

# get a list of all .java files in the apollo/ package
my $find = "find $apolloRoot/src/java/apollo/ -name '*.java' -print";

# files in which each regex appears (indexed by regex)
my $matches = {};

my @javaFiles = `$find`;
my $njf = scalar(@javaFiles);

foreach my $jf (@javaFiles) {
    chomp($jf);
    my $fh = FileHandle->new();
    my $lnum = 0;

    $fh->open($jf) || die "unable to read from $jf";
    while (my $line = <$fh>) {
	chomp($line);
	++$lnum;

	foreach my $regex (@REGEXES) {
	    if ($line =~ /$regex/) {
		&recordMatch($matches, $jf, $regex, $lnum);
	    }
	}
    }
}

# report results
foreach my $regex (@REGEXES) {
    my $byFile = $matches->{$regex};
    my @files = keys %$byFile;
    my @sortedFiles = sort { scalar(@{$byFile->{$b}}) <=> scalar(@{$byFile->{$a}}) } @files;
    my $nf = scalar(@sortedFiles);

    my $sum = 0;
    print "regex='$regex' ($nf files)\n";
    foreach my $file (@sortedFiles) {
	my $hits = $byFile->{$file};
	my $count = scalar(@$hits);
	printf("%6s %s\n", $count, $file);
	$sum += $count;
    }
    printf("%6s %s\n", $sum, 'TOTAL');
    print "\n";
    
}

exit(0);

# ------------------------------------------------------------------
# Subroutines
# ------------------------------------------------------------------

sub recordMatch {
    my($matches, $file, $regex, $linenum) = @_;
    my $byFile = $matches->{$regex};
    $byFile = $matches->{$regex} = {} if (!defined($byFile));
    my $hits = $byFile->{$file};
    $hits = $byFile->{$file} =[] if (!defined($hits));
    push(@$hits, $linenum);
}
