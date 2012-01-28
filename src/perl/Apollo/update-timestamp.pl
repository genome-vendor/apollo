# Run by calling perl
#
# Update the Apollo timestamp file based on the date of the latest change
# to the Apollo source.
# This script will be run by the Makefile when anything is compiled.
# Saves a file called APOLLO_ROOT/data/timestamp that is included in the jar
# and can be accessed by ApolloFrame.

my $APOLLO_ROOT = $ENV{APOLLO_ROOT};
if (!$APOLLO_ROOT) {
    $APOLLO_ROOT = $0;
    $APOLLO_ROOT =~ s/src.*//;
}

my $timestampfile = "$APOLLO_ROOT/data/timestamp";

open(OUT, ">$timestampfile") || die("Can't write to $timestampfile");

my $newest = `/bin/ls -lt $APOLLO_ROOT/conf/apollo.cfg $APOLLO_ROOT/src/java/apollo/*/*java $APOLLO_ROOT/src/java/apollo/*/*/*java $APOLLO_ROOT/src/java/apollo/*/*/*/*java | head -1`;

# Examples of ls format:
# -rw-rw-r--    1 nomi     software     3513 Aug 14  2001 src/java/apollo/gui/SwingWorker.java
# -rw-rw-r--    1 nomi     software    11216 Nov  7 03:43 src/java/apollo/gui/TablePanel.java
my @fields = split(' ',  $newest);
if (!($fields[7] =~ /\d{4}/)) {  # See if we need to add year
    my $year = scalar localtime;
    $year =~ s/.* //;
#    print "fields[7] = $fields[7], year = $year\n"; # DEL
    $fields[7] = "$year, $fields[7]";
}
my $date = "$fields[5] $fields[6] $fields[7]";

print "Updating $timestampfile to reflect newest Apollo file:\n $newest\n";

print OUT $date . "\n";
close(OUT);
