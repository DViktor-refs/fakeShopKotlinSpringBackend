package com.fakeshop.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class InsufficientStockException(message: String) : RuntimeException(message)
