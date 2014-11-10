#!/usr/bin/perl -w 
# 
# Simple minding program to read a list of fasta files (which must include a 1kp sample ID in their filename)
# and process each sequence in that file looking for:
# 1) suitable bias in their composition eg. %PAST as proxy for AGP
# 2) minimum length 90AA
# 3) minimum 10% proline across entire protein (including signal sequences)
# 
# Sequences which pass these tests are output to the file specified by -o (must be specified), with the sample ID prepended.
# These sequences are then given to the MAAB pipeline for processing and, if suitable, classification.
#
# Author: Andrew Cassin, 2014
#
# External Requirements: BioPerl, Perl
#
# Usage: perl  screen_hrgp.pl -o output_biased_sequences.fasta [-v]
#
use strict;
use Bio::SeqIO;
use Getopt::Std qw( getopts );

getopts('o:v');

my $total_ok = 0;
my $total_processed = 0;
my $total_files = 0;
open(OUT, ">$main::opt_o");
for my $input_proteome (@ARGV) {
	my $processed = 0;
	my $ok = 0;
	die "No 1kp SAMPLE ID for $input_proteome!\n" unless ($input_proteome =~ m#/([A-Z]{4})_#);
	my $sample_id = $1;
	print STDERR "Processing 1kp sample proteome for $sample_id\n";
	open(INPUT, "<$input_proteome") or die "Cannot open $input_proteome: $!\n";
	$total_files++;
	my $in = Bio::SeqIO->new( -fh => \*INPUT, -format => 'Fasta' );
	while (my $seq = $in->next_seq) {
		my $s = $seq->seq();
		chomp($s);
		$processed++;
		$total_processed++;

	   	my $proline   = ($s =~ tr/P/P/);
		my $alanine   = ($s =~ tr/A/A/);
		my $serine    = ($s =~ tr/S/S/);
		my $threonine = ($s =~ tr/T/T/);
		my $valine    = ($s =~ tr/V/V/);
		my $lysine    = ($s =~ tr/K/K/);
		my $tyrosine  = ($s =~ tr/Y/Y/);
		my $length    = length($s);

		my $pc_past = ($proline + $alanine + $serine + $threonine) * 100 / $length;
		my $pc_psky = ($proline + $serine + $lysine + $tyrosine) * 100 / $length
	;
		my $pc_pvyk = ($proline + $valine + $tyrosine + $lysine) * 100 / $length;
		my $pc_proline = ($proline * 100) / $length;
		next unless ($length >= 90);
		next unless ($pc_proline >= 10.0);
		next unless ($pc_past >= 45.0 || $pc_psky >= 45.0 || $pc_pvyk >= 45.0);
		$ok++;
		$total_ok++;
		print OUT ">" . $sample_id . "_" . $seq->id() . "\n" . $s . "\n";
	}
}
close(OUT) or die "Cannot close $main::opt_o: $!\n";
print STDERR "Processed $total_files input proteomes for putative HRGP.\n";
print STDERR "Processed $total_processed sequences, accepted $total_ok.\n";
print STDERR "Run complete.\n";
exit(0);
