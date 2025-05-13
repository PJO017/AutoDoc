package parser

type IR struct {
	Models    []ModelData    `json:"models"`
	Endpoints []EndpointData `json:"endpoints"`
}

type ModelData struct {
	Name             string                 `json:"name"`
	Description      string                 `json:"description"`
	Fields           []FieldData            `json:"fields"`
	IsInterface      bool                   `json:"isInterface"`
	IsEnum           bool                   `json:"isEnum"`
	ExtendsList      []string               `json:"extendsList"`
	ImplementsList   []string               `json:"implementsList"`
	Example          string                 `json:"example,omitempty"`
	Deprecated       bool                   `json:"deprecated"`
	DeprecationNotes string                 `json:"deprecationNotes,omitempty"`
	Since            string                 `json:"since,omitempty"`
	Extensions       map[string]interface{} `json:"extensions,omitempty"`
}

type FieldData struct {
	Name             string                 `json:"name"`
	Required         bool                   `json:"required"`
	Description      string                 `json:"description"`
	TypeRef          TypeRefData            `json:"typeRef"`
	ValidationRules  map[string]interface{} `json:"validationRules,omitempty"`
	Example          string                 `json:"example,omitempty"`
	Deprecated       bool                   `json:"deprecated"`
	DeprecationNotes string                 `json:"deprecationNotes,omitempty"`
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
	Deprecated      bool         `json:"deprecated"`
}

type Parameter struct {
	Name        string      `json:"name"`
	In          string      `json:"in"`
	Required    bool        `json:"required"`
	Description string      `json:"description"`
	TypeRef     TypeRefData `json:"type"`
}
