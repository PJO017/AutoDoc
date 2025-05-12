package parser

type IR struct {
	Models    []ModelData    `json:"models"`
	Endpoints []EndpointData `json:"endpoints"`
}

type ModelData struct {
	Name        string      `json:"name"`
	Description string      `json:"description"`
	Fields      []FieldData `json:"fields"`
}

type FieldData struct {
	Name        string      `json:"name"`
	Required    bool        `json:"required"`
	Description string      `json:"description"`
	TypeRef     TypeRefData `json:"typeRef"`
}

type TypeRefData struct {
	Base string        `json:"base"`
	Args []TypeRefData `json:"args"`
}

type EndpointData struct {
	Path            string       `json:"path"`
	Method          string       `json:"method"`
	Summary         string       `json:"summary"`
	Description     string       `json:"description"`
	Tags            []string     `json:"tags"`
	Parameters      []Parameter  `json:"parameters"`
	RequestBodyType *TypeRefData `json:"requestBodyType"`
	ResponseType    TypeRefData  `json:"responseType"`
}

type Parameter struct {
	Name        string      `json:"name"`
	In          string      `json:"in"`
	Required    bool        `json:"required"`
	Description string      `json:"description"`
	TypeRef     TypeRefData `json:"type"`
}
