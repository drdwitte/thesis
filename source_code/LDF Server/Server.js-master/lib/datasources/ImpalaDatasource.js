var Datasource = require('./Datasource'),
	http = require('http'),
	querystring = require('querystring'),
    N3 = require('n3'),
    request_ = require('request'),
    LRU = require('lru-cache'),
    Readable = require('stream').Readable;

var ENDPOINT_ERROR = 'Error accessing Impala endpoint';

function ImpalaDatasource(options) {
    if(!(this instanceof ImpalaDatasource))
        return new ImpalaDatasource(options);
    Datasource.call(this, options);

    // Set endpoint URL
    options = options || {};
    this._endpointHost = (options.endpointHost || 'localhost').replace(/[\?#][^]*$/, '');
    this._endpointPort = (options.endpointPort || 5000);
	this._endpointPath = '/impala';
}

Datasource.extend(ImpalaDatasource, ['triplePattern', 'limit', 'offset', 'totalCount']);

// Writes the results of the query to the given triple stream
ImpalaDatasource.prototype._executeQuery = function (query, tripleStream, metadataCallback) {

	// Send a count request
	this._doTotalCount(query, metadataCallback);

	// Create data for GET request
	var data = {q: this._createTriplePattern(query, false)};
	var queryString = querystring.stringify(data);

	var options = {
		host: this._endpointHost,
		port: this._endpointPort,
		path: this._endpointPath + '?' + queryString,
		method: 'GET',
		headers: {
      			'Accept': 'application/json'
    		}
	}
	var req = http.request(options, function(res) {
		res.setEncoding('utf8');
		res.on('data', function(chunk) {	
			var result = JSON.parse(chunk);
			for(var idx in result) {
				tripleStream.push(parseTriple(result[idx]));
			}
		});
		res.on('end', function() {
			tripleStream.push(null);
		});
	});
	
	req.end();
	req.on('error', function(e) {
  		console.log('problem with request: ' + e.message);
	});

	// Emits an error on the triple stream
    function emitError(error) {
        error && tripleStream.emit('error', new Error(ENDPOINT_ERROR + ' ' + self._endpoint + ': ' + error.message));
    }
};

// Input something like:
//  {"predicate": "testPredicate", "object": "testObject", "subject": "testSubject"}
// Output: JSON object
function parseTriple(triple) {
	var tripleResult = {};
	
	// Subject
	tripleResult["subject"] = triple.subject.trim();
	if(tripleResult["subject"][0] == '<' && tripleResult["subject"][tripleResult["subject"].length - 1] == '>') {
		tripleResult["subject"] = tripleResult["subject"].substring(1, tripleResult["subject"].length - 1);
	}
	
	// Predicate
	tripleResult["predicate"] = triple.predicate.trim();
	if(tripleResult["predicate"][0] == '<' && tripleResult["predicate"][tripleResult["predicate"].length - 1] == '>') {
		tripleResult["predicate"] = tripleResult["predicate"].substring(1, tripleResult["predicate"].length - 1);
	}
	
	// Object
	tripleResult["object"] = triple.object.trim();
	if(tripleResult["object"][0] == '<' && tripleResult["object"][tripleResult["object"].length - 1] == '>') {
		tripleResult["object"] = tripleResult["object"].substring(1, tripleResult["object"].length - 1);
	}

	return tripleResult;

}

ImpalaDatasource.prototype._doTotalCount = function(query, metadataCallback) {
	// Create the HTTP request
	var data = {q: this._createTriplePattern(query, true)};
	var queryString = querystring.stringify(data);

	var options = {
		host: this._endpointHost,
		port: this._endpointPort,
		path: this._endpointPath + '?' + queryString,
		method: 'GET',
		headers: {
      		'Accept': 'application/json'
    	}
	}

	var req = http.request(options, function(res) {
    	res.setEncoding('utf8');
    	res.on('data', function (chunk) {
			metadataCallback({ totalCount: JSON.parse(chunk)[0].count });
    	});
  	});
	
	req.on('error', function(e) {
    	console.log('problem with request: ' + e.message);
  	});
	
  	req.end();
};

// Creates a SQL pattern for the given triple pattern
ImpalaDatasource.prototype._createTriplePattern = function (triple, count) {
    if(count) {
		var query = "SELECT count(*) as count FROM temp_table ";
	} else {
		var query = "SELECT subject, predicate, object FROM temp_table ";
	}
    var queryFilter = "WHERE "
    var queryFilterAvailable = false;

    // Add a possible subject IRI
    if(triple.subject) {
        queryFilter += 'subject = "<' + triple.subject + '>" ';
        queryFilterAvailable = true;
    }

    // Add a possible predicate IRI
    if(triple.predicate) {
        if(queryFilterAvailable) {
 	    queryFilter += "AND "
	}
        queryFilter += 'predicate = "<' + triple.predicate + '>" ';
        queryFilterAvailable = true;
    }

	if(triple.object) {
            if(queryFilterAvailable) {
 	        queryFilter += "AND "
	    }
            queryFilterAvailable = true;
		// Add a possible object IRI or literal
		if (N3.Util.isIRI(triple.object)) {
                        queryFilter += 'object = "<' + triple.object + '>" ';
		} else if (!(literalMatch = /^"([^]*)"(?:(@[^"]+)|\^\^([^"]+))?$/.exec(triple.object))) {
		    noop();
		} else {
            var queryObject = triple.object.replace(/\"/g, '\\"');
            queryObject = queryObject.replace(/\'/g, "\\'");
			queryFilter += 'object = "' + queryObject + '" ';
		}
	}
    
    if(queryFilterAvailable) {
        query += queryFilter;
    } 

	if(!count && (triple.limit || triple.offset)) {
		query += "ORDER BY subject "
		if(!triple.limit) {
			//query += "LIMIT 10 ";
		} else {
    		query += "LIMIT " + triple.limit + " ";
		}
    	if(!triple.offset) {
			//query += "OFFSET 0 ";
    	} else {
			query += "OFFSET " + triple.offset + " ";
    	}
	}
    query += ";";

    console.log(query);
    
    return query;
};

// The empty function
function noop() {}

module.exports = ImpalaDatasource;
