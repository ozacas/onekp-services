#!/usr/bin/perl -w
#
use strict;
use Getopt::Std qw( getopts );

getopts('o:vp:');
my $prefix = $main::opt_p;
$prefix = "" unless (defined($main::opt_p));
die "Cannot save putative protein set - no -o <output folder>!\n" 
	unless (defined($main::opt_o));
$prefix .= "_" if (length($prefix) > 0);

my $getorf = $ENV{'HOME'} . "/shared/emboss/bin/getorf";
my $attempted = 0;
my $extracted = 0;
print STDERR "Saving ORFs to $main::opt_o\n" if (defined($main::opt_v));
print STDERR "Running $getorf to perform extraction.\n" if (defined($main::opt_v));

for my $input_transcriptome (@ARGV) {
	die "No 1kp sample ID!\n" unless ($input_transcriptome =~ m#^([A-Z]{4})/.*$#);
	my $onekp_sample_id = $1;

	my $protein_out = $main::opt_o . "/" . $prefix . $onekp_sample_id . "_orfs.min200nt.fasta";
	print STDERR "Extracting orfs from $input_transcriptome...\n" if (defined($main::opt_v));
	$attempted++;

	die "Will not overwrite existing $protein_out!\n" if (-r $protein_out);

	my $status = system("$getorf -find 1 -minsize 200 -sequence $input_transcriptome -outseq $protein_out");
	if ($status == 0 && ! -z $protein_out) {
		$extracted++;		
		print STDERR "Saved ORFs to $protein_out\n" if (defined($main::opt_v));
	}
}
print STDERR "Processed $attempted RNA-seq assembled transcriptomes.\n";
print STDERR "Successfully extracted ORFs from $extracted transcriptomes.\n";
print STDERR "Run completed.\n";
exit(0);
