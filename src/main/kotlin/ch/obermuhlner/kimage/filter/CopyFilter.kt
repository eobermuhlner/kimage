package ch.obermuhlner.kimage.filter

class CopyFilter : MatrixImageFilter({_, source -> source.copy() })