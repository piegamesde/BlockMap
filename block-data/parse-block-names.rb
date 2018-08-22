while line = gets
	line = line.gsub(/\[\[[^\|\]]+\|([^\]]+)\]\]/,'\1')
	line = line.gsub('[','').gsub(']','')
	if line =~ /\{\{dec-hex\|(\d+)\}\}\s*\|\|\s*([^\|<#]+)/
		num = $1.to_i
		name = $2.strip
		puts "0x%04X\t%s" % [num,name]
	end
end
