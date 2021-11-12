# Generate source values for 'enum class IndexOffence(val label: String, val group: IndexOffenceGroup)'
# Execute as e.g.:
# 1. start with s/sheet/.xls export of LOV table: expect tab per key, e.g. 2nd tab for offence groups
# 2. edit in e.g. Numbers to (i) replace "," with "|"; (ii) delete row 0 column headings;
# 3. export to CSV; file per sheet created e.g. as/renamed as "LOV-IndexOffencesExportBarForComma.csv"
# awk -f offences.awk < LOV-IndexOffencesExportBarForComma.csv | iconv -c --to-code=UTF8 > lov-index-offences.csv
# Note the use of `iconv` here as the output of awk on this input included some non-UTF8 characters
BEGIN {FS = ","}
{
index_offence_name=$1
index_offence_key=toupper($1)
gsub("\&","AND",index_offence_key);
gsub("[^ -~]+","_",index_offence_key);
gsub("\\|","",index_offence_key);
gsub("\\.|:|\\(|\\)|\"|\'|\`|\‘","",index_offence_key);
gsub("[[:space:]]","_",index_offence_key);
gsub("[-–/]","_",index_offence_key);
gsub("_+","_",index_offence_key);
offence_group_key=toupper($2)
if( length(offence_group_key) == 0 ) {offence_group_key="NOT_SPECIFIED"}
gsub("\&","AND",offence_group_key);
gsub("[[:space:]]","_",offence_group_key);
gsub("[-–]","_",offence_group_key);
gsub("_+","_",offence_group_key);
gsub("\\.|\"","",index_offence_name);
gsub("[-–]","-",index_offence_name);
gsub("\\|",",",index_offence_name);
gsub("\xA0"," ",index_offence_name);
printf("  %s\(\"%s\", IndexOffenceGroup.%s_GROUP\),\n", index_offence_key, index_offence_name, offence_group_key);
}