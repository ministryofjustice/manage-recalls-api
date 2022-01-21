# Audit

We are modelling our audit on https://wiki.postgresql.org/wiki/Audit_trigger

The rationale was that rather than auditing each iteration of a recall, we needed to be able to audit each field.

The data is stored as json, with string representations of the values (using json and hstore). 
These values are then translated back to their original data type within the API code. 
Currently, this uses reflection and the hibernate persister to match the database column names to the field on the
Recall and find the data type. 

We are, however, aware that this can cause problems going forwards if the structure of the recall changes and is
not backwards compatible. Any fields that are removed would need to be manually mapped in the API code to ensure 
that old recall audits can still be viewed. 

The recallReasons field is stored as a collection of strings in a separate table, as such, this (and any other collections -
addresses?) will require their own audit table, and trigger.
