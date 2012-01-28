#!/usr/local/bin/perl
# Use as a pipe from stdin to stdout (e.g. fix-xml.pl <bad.xml >good.xml).
# Fixes some XML problems.
# 1. Fixes the erroneous <type> lines created by Apollo v1.3.3--transcripts
# and exons had type "gene".
# 2. Gets rid of any control characters that we find, since they make Apollo unhappy.
# 3. The aa sequence name was a copy of the cdna sequence name.  Should be
# -PA instead of -RA.
# Before:
#       <seq id="CG:temp1-RA" length="285" type="cdna">
#       <seq id="CG:temp1-RA" length="35" type="aa">
# After:
#       <seq id="CG:temp1-RA" length="285" type="cdna">
#       <seq id="CG:temp1-PA" length="35" type="aa">


my $fs;
my $name;
my $type;
my $line;
while ($line = <>) {
    # Can't seem to get rid of ^[s without removing legitimate newlines, so
    # temporarily change the newlines to something distinctive, then change back
    # when we're done stripping out control characters.
    $line =~ s/\n/XXXNEWLINEXXX/g;  # protect newlines
    $line =~ tr/\000-\037//d;    # remove any control characters (including newlines!)
    $line =~ s/XXXNEWLINEXXX/\n/;  # Restore legitimate newlines

    if ($line =~ /\<feature_s/) {
	$fs = $line;
	$name = <>;
	if (!($name =~ /CG/ || $fs =~ /CT/)) {
#	    printf STDERR "hey, name doesn't contain CG: name = $name and fs = $fs";  # DEL
	    if ($name =~ /type/ && $fs =~ /\<feature_span\>/) {
		$type = $name;
		$name = "";
	    }
	    else {
		print "$fs$name";
		next;
	    }
	}
	else {
	    $type = <>;
	}
	if (!($type =~ /gene/)) {
#	    printf STDERR "hey, type doesn't contain gene. type = $type and name = $name\n";  # DEL
	}
	else {
	    if ($fs =~ /feature_set/) {
		$type =~ s/gene/transcript/;
#		printf STDERR "Ok, set type to transcript for $name";
	    }
	    elsif ($fs =~ /feature_span/) {
		$type =~ s/gene/exon/;
#		printf STDERR "Ok, set type to exon for $name";
	    }
	    else {
#		printf STDERR "Hey, fs is weird: $fs";
	    }
	}
	print "$fs$name$type";
	$fs = $name = $type = "";
    }
    # Fix protein sequence ID
    elsif ($line =~ /seq id[^ ]+(\-R).*type=\"aa\"/) {
	printf STDERR "Changing line.  Before: $line";
	$line =~ s/$1/\-P/;
	printf STDERR "After: $line";
	print $line;
    }
    else {
	print $line;
    }
}



