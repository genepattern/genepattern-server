#converts a windows, mac, or unix file to the host line ending type

$input_file = shift;
$output_file = shift;
open(F1, "$input_file") || die "cannot open input file $input_file \n";
$output_file = ">" . "$output_file";
open(OUT, $output_file) || die "cannot open output file $output_file \n";
while (<F1>) {
	s/\r\n|\r|\n/$\//g; # windows | mac | unix
	print OUT $_;
}

close F1;

