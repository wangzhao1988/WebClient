pdf("distribution.pdf", width=6, height=6)

data <- read.table("times.txt", header=T, sep="\t")

h<-hist(data, breaks=20, col="blue", xlab="Milliseconds per Request", 
   main="Frequency for each time bucket") 
xfit<-seq(0, 1020,length=80) 
yfit<-dnorm(xfit,mean=mean(data),sd=sd(data)) 
yfit <- yfit*diff(h$mids[1:2])*length(data) 
# lines(xfit, yfit, col="blue", lwd=2)s
dev.off()