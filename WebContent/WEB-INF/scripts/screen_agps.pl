#!/usr/bin/perl -w
# Simple-minded bioperl script to extract possible AGP's (really
# compositionally biased proteins) from the input fasta.
#
# Usage: perl screen_agps.pl <input fasta> 
#
# Suitable sequences are output to stdout
#
# Author: Andrew Cassin, 2014
#
use Bio::SeqIO;

my $in = Bio::SeqIO->new( -fh => \*STDIN, -format => 'Fasta' );

while (my $seq = $in->next_seq) {
	my $s = $seq->seq();
	my $proline = ($s =~ tr/P/P/);	
	my $alanine = ($s =~ tr/A/A/);
	my $serine = ($s =~ tr/S/S/);
	my $threonine = ($s =~ tr/T/T/);
	my $length = length($s);

	my $pc_past = ($proline + $alanine + $serine + $threonine) * 100 / $length;
	my $pc_proline = ($proline * 100)/ $length;

	next unless ($pc_past >= 30.0 && $pc_proline >= 14.0 &&  $length > 100);
	print ">" . $seq->id() . "\n" . $seq->seq() . "\n";
}
exit(0);
