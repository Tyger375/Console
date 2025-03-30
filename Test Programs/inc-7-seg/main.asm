	org 0
main:
	nop
	nop
	xor a
	ld HL, 0xF000
loop:
	inc a
	ld (HL), a
	jr loop