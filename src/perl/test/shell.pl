#!/usr/bin/perl -w

use CORBA::ORBit idl => [ '../../../idl/apollo.idl' ];
use Error qw(:try);

sub shell {
    print "Welcome to the Apollo IDL shell interface!\n";
    require Term::ReadLine;
    #	import Term::ReadLine;
    my $term = shift || Term::ReadLine->new('Apollo Monitor');
    my $prompt = "apollo> ";
    my $quit = 0;
    my $dbh;
    my $session;
    my $r;
    my $rv;
    my @rvs = ();
    my @lines = ();
    my $end_signal = "";
    my $ior_file = "apollo_server.ior";
    my @param_list = ();

    while (!$quit) {
	no strict "vars";
	if ($end_signal) {
	    @lines = ($lines);
	    while ($end_signal && ($line = $term->readline("? "))) {
		if($line !~ /$end_signal/) {
		    $lines[0].= "\n$line";
		}
		else {
		    $end_signal = "";
		}
	    }
	    next;
	}
	my $line = 
	  @lines ? shift @lines : $term->readline($prompt);
	my ($cmd, @w) = split(' ',$line);
	my $rest = join(" ", @w);
	$_ = $cmd;
	s/ *//; # rid of beginning ws
	s/^p$/print/;
	if (/^url/){
	    my $url = $rest;
	    $ior_file = "remote.ior";
	    my $cmd = "wget -O $ior_file $url";
	    print "cmd=$cmd\n";
	    print `$cmd`;
	}
	elsif (/^par$/) {
	    push(@param_list, {name=>(shift @w), value=>join(" ", @w)});
	    map {printf "%s=%s\n",$_->{name}, $_->{value}} @param_list;
	}
	elsif (/^xml$/) {
	    @lines=("par source xml");
	}
	elsif (/^ior$/) {
	    $ior_file = $rest;
	}
	elsif (/^c$/) {
	    $orb = CORBA::ORB_init("orbit-local-orb");

	    open(F,"$ior_file") || die "Could not open $ior_file";
	    $ior = <F>;
	    chomp $ior;
	    close(F);
 
	    $mgr = $orb->string_to_object($ior);
	    
	    $session = $mgr->initiate_Session([]);
	    $session->connect(\@param_list);
	    print "connected! (use variable \$session)\n";
	}
	elsif (/^g$/) {
	    if (!$session) {
		@lines = ("c", "$_");
	    }
	    else {
		eval {
		    $annreg = 
		      $session->get_AnnotatedRegion($rest);
		};
		if ($@) {
		    print "uh oh - \n$@";
		}
		else {
		    print "got result in var \$annreg";
		}
	    }
	}
	elsif ($annreq && $annreg->can($_)) {
	    eval {
#		my @args = (eval "$rest");
		$r = $annreg->$_($rest);
	    };
	    if ($@) {
		print "$annreg error:\n$@\n";
	    }
	    else {
		dd($r);
		print "Got:$r\n";
	    }
	}
	elsif ($session && $session->can($_)) {
	    eval {
#		my @args = (eval "$rest");
		$r = $ad->$_($rest);
	    };
	    if ($@) {
		print "$session error:\n$@\n";
	    }
	    else {
		print "Got:$r\n";
	    }
	}
	elsif (/^f$/) {
	    open(F, $rest);
	    @lines = <F>;
	    close(F);
	}
	elsif (/^\<\</) {
	    $end_signal = $rest;
	}
	elsif (/^tbl$/) {
	    open(F, $rest) || warn("can't find $rest");
	    @rows = ();
	    %rowh = ();
	    while(<F>) {
		chomp;
		my $row = [split(/\t/, $_)];
		push(@rows, $row);
		$rowh{$row->[0]} = $row;
	    }
	    print "\@rows: last row index: $#rows\n";
	    close(F);
	}
	elsif (/^quit$/) {
	    $quit = 1;
	}
	elsif (/^reload$/) {
	    open(F, $INC{"GxAdapters/GxRoot.pm"}) 
	      || warn("cant find it in @INC");
	    my $t = join('',<F>);
	    close(F);
	    eval "$t";
	    if ($@) {
		print $@;
	    }
	    else {
		print "reloaded!\n";
	    }
	    shell($term);
	    return;
	}
	else {
	    $rv = eval "$line";
	    if ($@) {
		print "error:\n";
		print $@;
	    }
	    else {
		dd($rv);
		print "\n$rv\n";
	    }
	}
	if ($r) {
	    if (!@rvs || $rv != $rvs[-1]) {
		push(@rvs, $r);
	    }
	}
	print "\n";
#	print "you said $_ \n";
    }
    
}

shell();
exit 0;



#--

sub dd {
    require "Data/Dumper.pm";
    my $ob = shift;
    my $d = Data::Dumper->new(['ob', $ob]);
    print $d->Dump;
}
