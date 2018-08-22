blocks = []

open( 'block-names.txt', 'r' ) do |s|
	while line = s.gets
		line = line.strip
		line =~ /^(0x[0-9A-F]{4})\t(.*)$/
		num = $1
		name = $2
		blocks << [num,name]
	end
end

for (num,name) in blocks
	off = nil
	while off == nil
		STDERR.print "Offset of %s> " % name
		off = gets
		off = off.strip
		if off =~ /(\d+),?\s*(\d+)/
			off = [$1.to_i, $2.to_i]
		elsif off == 'x'
			off = false
		else
			STDERR.puts "Invalid offset spec '%s'" % off
			off = nil
		end
	end
	if off
		#spec = "dup 0x%02X 0x%02X 16 16 sub-image average-color 0x%02X swap print-block-color # %s" % [off[0]*16,off[1]*16,num,name]
		spec = "0x%04X\t%d,%d\t# %s" % [num,off[0],off[1],name]
		STDERR.puts spec
		puts spec
	end
end
