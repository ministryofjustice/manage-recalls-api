# Generate source values for 'enum class IndexOffenceGroup(val label: String)'
# Execute as e.g.:
# 1. start with s/sheet/.xls export of LOV table: expect tab per key, e.g. 2nd tab for offence groups
# 2. edit in e.g. Numbers to delete row 0 column headings;
# 3. export to CSV; file per sheet created
# awk -f groups.awk < LOV\ -\ Offence\ groups-Table\ 1.csv > lov-index-offence-groups.csv
# Note '–' char which does not match . and hence edited out specifically from key and name!
BEGIN {FS = ","}
{
offence_group_name=$1
offence_group_key=toupper($1)
gsub("\&","AND",offence_group_key);
gsub("[[:space:]]","_",offence_group_key);
gsub("[-–]","_",offence_group_key);
gsub("_+","_",offence_group_key);
printf("  %s_GROUP\(\"%s\"\),\n", offence_group_key, offence_group_name);
}
