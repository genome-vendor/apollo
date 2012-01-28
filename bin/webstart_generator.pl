#!/usr/bin/perl

use strict;
use warnings;

use XML::Twig;
use Getopt::Long qw(:config bundling no_ignore_case);
use File::Basename;
use IO::File;

my $input;
my $input_jar_dir;
my $output;
my $output_jar_dir;
my $url;
my $jar_href;

#jar signer options
my $alias;
my $keypass;
my $storepass;
my $keystore;
my $validity;
my $common_name;
my $organization_unit;
my $organization_name;
my $locality_name;
my $state_name;
my $country;

#jnlp
my $xml_declaration;
my $jnlp;

parse_options();
parse_xml();
sign_jars();
generate_jnlp();

sub print_usage
{
  my $prog = basename($0);
  die << "END";
usage: $prog --input|-i <webstart_config.xml>
             --input_jar_directory|-d <input_jar_directory>
             --output|-o <output.jnlp>
             --output_jar_directory|-D <output_jar_directory>
             [--help|-h]

       -i: XML file with the webstart configuration
       -d: directory of jar files to be signed
       -o: output generated jnlp file
       -D: directory to write signed jars to
       -h: help
END
}

sub parse_options
{
  my %opts = ();
  GetOptions(\%opts, "input|i=s", "input_jar_directory|d=s",
             "output|o=s", "output_jar_directory|D=s",
             "jar_href|j=s",
             "help|h");
  print_usage() if $opts{help};
  $input = $opts{input} || die "Missing input webstart XML configuration\n";
  $input_jar_dir = $opts{input_jar_directory} ||
    die "Missing input jar directory\n";
  $output = $opts{output} || die "Missing output jnlp file name\n";
  $output_jar_dir = $opts{output_jar_directory} ||
    die "Missing output signed jar directory\n";
}

sub parse_xml
{
  my $twig = new XML::Twig(twig_roots => { "jarsigner" => \&process_jarsigner,
                                           "jnlp" => sub { $jnlp = $_[1]; },
                                           "webserver" => \&process_webserver
                                         },
                           pretty_print => "indented");
  $twig->parsefile($input);
  $xml_declaration = $twig->xmldecl();
}

sub process_jarsigner
{
  my ($twig, $elt) = @_;
  $alias = $elt->first_child("alias")->text();
  $keypass = $elt->first_child("keypass")->text();
  $storepass = $elt->first_child("storepass")->text();
  $keystore = $elt->first_child("keystore")->text();
  $validity = $elt->first_child("validity")->text();
  $common_name = $elt->first_child("commonName")->text();
  $organization_unit = $elt->first_child("organizationUnit")->text();
  $organization_name = $elt->first_child("organizationName")->text();
  $locality_name = $elt->first_child("localityName")->text();
  $state_name = $elt->first_child("stateName")->text();
  $country = $elt->first_child("country")->text();
}

sub process_webserver
{
  my ($twig, $elt) = @_;
  $url = $elt->first_child("url")->text();
  $jar_href = $elt->first_child("jar_location")->text();
}

sub sign_jars
{
  if (!-e "$output_jar_dir") {
    mkdir($output_jar_dir) || die "Error creating output jar directory " .
    $output_jar_dir . ": $!" if !-e $output_jar_dir;
  }
  if (-e "$output_jar_dir/$keystore") {
    unlink("$output_jar_dir/$keystore");
  }
  my $cmd = "keytool -genkey -alias $alias -keypass $keypass " .
            "-storepass $storepass -keystore $output_jar_dir/$keystore " .
            "-validity $validity " .
            "-dname 'CN=$common_name, OU=$organization_unit, " .
            "O=$organization_name, L=$locality_name, S=$state_name, " .
            "C=$country'";
  system($cmd) == 0 || die "Error generating key: $!\n";
  $cmd = "jarsigner -keystore $output_jar_dir/$keystore " .
         "-storepass $storepass -keypass $keypass ";
  while (my $input_jar = <$input_jar_dir/*.jar>) {
    my $jar = basename($input_jar);
    print "Processing $jar\n";
    system($cmd . "-signedjar $output_jar_dir/$jar $input_jar $alias") == 0 ||
      die "Error signing jar file $jar: $!\n";
  }
}

sub generate_jnlp
{
  my $jnlp_file = basename($output);
  $jnlp->set_att(codebase => $url);
  $jnlp->set_att(href => $jnlp_file);
  my $resources = $jnlp->first_child("resources");
  while (my $jar_file = <$output_jar_dir/*.jar>) {
    my $jar = basename($jar_file);
    $resources->insert_new_elt("last_child",
                               "jar",
                               { href => "$jar_href/$jar" });
  }
  my $out = new IO::File($output, "w") ||
    die "Error writing jnlp file: $!";
  $out->print("$xml_declaration");
  $out->print($jnlp->sprint());
}
