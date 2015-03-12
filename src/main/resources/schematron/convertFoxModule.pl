use strict;
use File::Copy;
$| = 1;
my $fileName;
my $direction;

if (@ARGV ==2) {
  $fileName = $ARGV[0];
  $direction = $ARGV[1];
}
elsif(@ARGV == 0){
  print ("Enter File to convert\n");
  $fileName = <STDIN>;
  print ("Do you want old to new (o) or new to old (n)\n");
  #stdin adds a weird character that was not got by chomp probably some Windows rubbish
  $direction = <STDIN>;
  $fileName =~ s/^\s+|\s+$//g;
  $direction =~ s/^\s+|\s+$//g;
}
else {
  print "usage: convertFoxModule.pl fileLocation mode(o or n)";
  exit;
}
#read file
open(FILEIN,"<$fileName") || die("Cannot open file $fileName $!");
my @lines = <FILEIN>;
close(FILEIN);

#create backup first!
my $backupFile;
if($direction eq 'o'){
  $backupFile = "${fileName}.oldbak";
}elsif($direction eq 'n'){
  $backupFile = "${fileName}.newbak";
}
copy($fileName,$backupFile) or die "Copy failed: $!";

# write modified

open(FOUT,">$fileName")|| die("Cannot open file $fileName $!") ;

my $foxuri = 'xmlns:fox="http://www.og.dti.gov/fox"';
my $foxguri = 'xmlns:fox="http://www.og.dti.gov/fox_global"';
my $foxplaceholder = ':::FOX_NS:::';
my $foxgplaceholder = ':::FOXG_NS:::';
my $oldNamespaceString = '(xmlns:)([^"]*)(="http://www.og.dti.gov/fox)(_global)?(")';
my $newNamespaceString = '(xmlns:)([^"]*)(="http://www.og.dti.gov/fox)(_global)?[^"]*/[^"]*(")';
my $line;

foreach my $line (@lines){
   
   $line =~ s/$foxuri/$foxplaceholder/g;
   $line =~ s/$foxguri/$foxgplaceholder/g;
   if($direction eq 'o'){
       $line =~ s/$oldNamespaceString/$1$2$3$4\/$2$5/g;
   }elsif($direction eq 'n'){
       $line =~ s/$newNamespaceString/$1$2$3$4$5/g;
   }    
   $line =~ s/$foxgplaceholder/$foxguri/g;
   $line =~ s/$foxplaceholder/$foxuri/g;
   print FOUT $line;
    
}
close(FOUT);