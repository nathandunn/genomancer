
Modules:

Trellis:
	This is the core of the project, a modular DAS/2 server and
	servlet framework.  Implementations of the Trellis DAS/2
	model can be plugged into Trellis to create a fully functional DAS/2
	server.
	Includes core DAS/2 model API

Vine:
	DAS/2 --> DAS/2 proxy server
	A Trellis-based DAS/2 proxy server for other DAS/2 servers.
	Basically wraps a DAS/2 client, that populates Trellis DAS/2
	model, as a plugin for Trellis
	Includes DAS/2 client library (that implements Trellis core DAS/2 API)

Ivy:
	DAS1 --> DAS/2 proxy server
	A Trellis-based DAS/2 proxy server for DAS1 servers.  
	Basically wraps a DAS1 client, along with 
	conversion from Ivy DAS1 model to Trellis DAS/2 model,
	into a plugin for Trellis
	Includes core DAS1 model API
	Includes DAS1 client library 

Sillert:
	This is a DAS1 version of Trellis -- that is, a modular DAS1
	server and servlet framework.  Implementations of the
	Sillert DAS1 model can be plugged into Sillert to create a fully
	functional DAS1 server.
	(not yet being worked on);

Eniv:
	DAS1 --> DAS1 proxy server
	A Sillert-based DAS1 proxy server for other DAS1 servers.
	Basically wraps a DAS1 client (from Ivy), that populates Ivy DAS1 
	model, as a plugin for Sillert 
	(not yet being worked on)

Yvi:
	DAS/2 --> DAS1 proxy server
	A Sillert-based DAS1 proxy server for DAS/2 servers.
	Basically wraps a DAS/2 client (from Vine), along with
	conversion from	Trellis DAS/2 model to Ivy DAS1 model,
	into a plugin for Sillert
	(not yet being worked on)

Poka:
	UCSC genome database --> DAS/1 / DAS/2 / JSON server


Tengcha:
	A Trellis plugin that allows Trellis to pull data from a standard Chado 
	database. This plugin was developed assuming a standard Chado 
	database schema, and data loaded into Chado from GFF3 using the 
	GMOD bulk loader (gmod_bulk_load_gff3.pl).




