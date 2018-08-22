puts "'textures.png load-image"
puts "'default 0x80FF00FF\tprint-block-color"
puts "0x0000 0x00000000 print-block-color # Air"

while line = gets
	if line =~ /^(0x[0-9A-F]+)\t(\d+),(\d+)\t(#.*)$/
		num = $1
		x = $2.to_i
		y = $3.to_i
		comment = $4
		puts "dup 0x%02X 0x%02X 16 16 sub-image average-color 0x%04X swap print-block-color %s" % [x*16, y*16, num, comment]
	end
end
