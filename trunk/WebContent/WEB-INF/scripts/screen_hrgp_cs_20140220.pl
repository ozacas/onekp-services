#!/usr/bin/perl -w
use strict;
use Bio::SeqIO;

my $in = Bio::SeqIO->new( -fh => \*STDIN, -format => 'Fasta' );

while (my $seq = $in->next_seq) {
	my $s = $seq->seq();
	my $proline = ($s =~ tr/P/P/);	
	my $alanine = ($s =~ tr/A/A/);
	my $serine = ($s =~ tr/S/S/);
	my $threonine = ($s =~ tr/T/T/);
	my $valine = ($s =~ tr/V/V/);
	my $lysine = ($s =~ tr/K/K/);
	my $tyrosine = ($s =~ tr/Y/Y/);
	my $length = length($s);

	my $pc_past = ($proline + $alanine + $serine + $threonine) * 100 / $length;
	my $pc_psky = ($proline + $serine + $lysine + $tyrosine) * 100 / $length;
	my $pc_pvyk = ($proline + $valine + $tyrosine + $lysine) * 100 / $length;
	next unless ($pc_past >= 45.0 || $pc_psky >= 45.0 || $pc_pvyk >= 45.0);
	print ">" . $seq->id() . "\n" . $seq->seq() . "\n";
}
exit(0);
