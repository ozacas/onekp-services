#!/usr/bin/perl -w
# Simple-minded bioperl script to extract hits from (eg. blastp) TSV output
# file and then construct a fasta file with the database sequences hit
#
# Usage: extract_proteins.pl -b blast_hits.tsv -f input_fasta_file.fasta
#
# Output is to stdout, with summary messages to stderr.
#
# Author: Andrew Cassin, 2014
#
use strict;
use Getopt::Std qw( getopts );

getopts('b:f:v');
die "Usage: extract_proteins.pl -b <blast hit TSV file> -f <fasta to extract>!\n" unless (defined($main::opt_b) && defined($main::opt_f));

print STDERR "Loading blast hits from $main::opt_b\n";
open(BLAST, "<" . $main::opt_b) || die "Cannot open $main::opt_b: $!\n";
my $col = -1;
my %seqs_to_extract;
while (<BLAST>) {
	next if (m#^\s*$# || m/^#/);
	my(@F) = split;
	if ($col < 0) {
		print;
		for (my $i=0; $i < scalar(@F); $i++) {
			if ($F[$i] =~ m#^[A-Z]{4}_Locus#) {
				$col = $i; last;
			}
		}	
		print STDERR "Using column $col for 1kp scaffold ID's.\n";
	}	
	if ($col  >= 0) {
		$seqs_to_extract{$F[$col]} = 1;
	}
}
close(BLAST);
print STDERR "Blast hits loaded.\n";
print STDERR "Found " . scalar(%seqs_to_extract) . " sequences to extract.\n";

print STDERR "Processing fasta file... $main::opt_f\n";
open(FASTA, "<" . $main::opt_f) || die "Cannot open $main::opt_f: $!\n";
my $dump_seq = 0;
while (<FASTA>) {
	if (m#^>#) {
		$dump_seq = 0;
		my($id, $descr) = m#^>(\S+)\s+(.*)$#;
		if (exists($seqs_to_extract{$id})) {
			$dump_seq = 1;
			print;
		}
	} elsif ($dump_seq) {
		print;
	}
}
close(FASTA);

print STDERR "all done!";
exit(0);
