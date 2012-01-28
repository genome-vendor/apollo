#!/usr/bin/perl

# ------------------------------------------------------------------
# checkLog4JLoggerNames.pl
#
# By convention each Apollo Java class that uses Log4J contains
# a statement like this one, usually as the first declaration in 
# the class:
#
# protected final static Logger logger = LogManager.getLogger(Apollo.class);
#
# This script checks that the class name used in each of these 
# getLogger() calls matches the name of the enclosing file 
# (Apollo.java in this case), and prints a warning message for 
# those classes where this is not true.  (Log4J logger names do 
# not have to correspond to Java class names, but this is the 
# convention we are currently using in Apollo.)
#
# Note that this script is checking the logger names against the 
# file name, not against the name of the enclosing class (which
# may differ e.g., for inner classes.)  Essentially it's a 
# simple safeguard against typos in the getLogger() call.
# 
# $Revision: 1.1 $ $Date: 2007/01/02 20:39:36 $ $Author: jcrabtree $
# ------------------------------------------------------------------

use strict;
use FileHandle;
use File::Basename;

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

my @javaFiles = `$find`;
my $njf = scalar(@javaFiles);

my $numUsingLog4J = 0;
my $numWithMismatches = 0;

# perform a trivial check on each
foreach my $jf (@javaFiles) {
    chomp($jf);
    my $fh = FileHandle->new();
    my $lnum = 0;

    # get Java class name from filename
    my($fileName, $path, $suffix) = &File::Basename::fileparse($jf, (".java"));
    my $getLoggerLineNo = undef;
    my $hasMismatch = 0;

    $fh->open($jf) || die "unable to read from $jf";
    while (my $line = <$fh>) {
	chomp($line);
	++$lnum;

	# look for getLogger calls
	if ($line =~ /static\s+Logger\s+.*LogManager.getLogger\(([^\.]+)\.class\)/) {
	    my $className = $1;

	    if (defined($getLoggerLineNo)) {
		print "WARNING - duplicate getLogger calls in $jf (lines $getLoggerLineNo and $lnum)\n";
	    } else {
		$getLoggerLineNo = $lnum;
	    }

	    if ($className ne $fileName) {
		print "$jf line $lnum: $line\n";
		++$hasMismatch;
	    }
	}
    }

    ++$numUsingLog4J if (defined($getLoggerLineNo));
    ++$numWithMismatches if ($hasMismatch);
    
    $fh->close();
}

print "SUMMARY:\n";
printf("%4s %-4s Java files found in $apolloRoot/src/java/apollo\n", '', $njf);
printf("%4s/%-4s files call LogManager.getLogger()\n", $numUsingLog4J, $njf);
printf("%4s/%-4s files have one or more logger name mismatches.\n", $numWithMismatches, $numUsingLog4J);

exit(0);
