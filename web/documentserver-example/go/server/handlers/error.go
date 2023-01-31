package handlers

import "fmt"

type HandlerExistsError struct {
	Code int
}

func (e *HandlerExistsError) Error() string {
	return fmt.Sprintf("A Handler with code %d exists", e.Code)
}
