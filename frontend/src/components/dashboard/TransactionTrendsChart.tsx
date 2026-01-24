import { useEffect, useState } from "react";
import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";

interface TransactionTrendsProps {
  data?: Array<{
    date: string;
    count: number;
    amount: number;
  }>;
}

export default function TransactionTrendsChart({ data = [] }: TransactionTrendsProps) {
  const [chartData, setChartData] = useState({
    categories: [] as string[],
    countData: [] as number[],
    amountData: [] as number[],
  });

  useEffect(() => {
    if (data && data.length > 0) {
      const categories = data.map(item => {
        const date = new Date(item.date);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
      });
      const countData = data.map(item => item.count);
      const amountData = data.map(item => Math.round(item.amount / 1000)); // Convert to thousands

      setChartData({ categories, countData, amountData });
    } else {
      // Default demo data
      setChartData({
        categories: ['Jan 1', 'Jan 8', 'Jan 15', 'Jan 22', 'Jan 29'],
        countData: [45, 52, 38, 65, 58],
        amountData: [120, 145, 110, 180, 165],
      });
    }
  }, [data]);

  const options: ApexOptions = {
    legend: {
      show: true,
      position: "top",
      horizontalAlign: "left",
    },
    colors: ["#465FFF", "#22C55E"],
    chart: {
      fontFamily: "Outfit, sans-serif",
      height: 310,
      type: "line",
      toolbar: {
        show: false,
      },
    },
    stroke: {
      curve: "smooth",
      width: [3, 3],
    },
    fill: {
      type: "gradient",
      gradient: {
        opacityFrom: 0.15,
        opacityTo: 0,
      },
    },
    markers: {
      size: 0,
      strokeColors: "#fff",
      strokeWidth: 2,
      hover: {
        size: 6,
      },
    },
    grid: {
      xaxis: {
        lines: {
          show: false,
        },
      },
      yaxis: {
        lines: {
          show: true,
        },
      },
    },
    dataLabels: {
      enabled: false,
    },
    tooltip: {
      enabled: true,
      x: {
        show: true,
      },
      y: {
        formatter: function (value, { seriesIndex }) {
          if (seriesIndex === 0) {
            return `${value} transactions`;
          }
          return `$${value}K`;
        },
      },
    },
    xaxis: {
      type: "category",
      categories: chartData.categories,
      axisBorder: {
        show: false,
      },
      axisTicks: {
        show: false,
      },
    },
    yaxis: [
      {
        title: {
          text: "Transactions",
          style: {
            fontSize: "12px",
            fontWeight: 400,
          },
        },
        labels: {
          formatter: function (value) {
            return Math.round(value).toString();
          },
        },
      },
      {
        opposite: true,
        title: {
          text: "Amount ($K)",
          style: {
            fontSize: "12px",
            fontWeight: 400,
          },
        },
        labels: {
          formatter: function (value) {
            return `$${Math.round(value)}K`;
          },
        },
      },
    ],
  };

  const series = [
    {
      name: "Transaction Count",
      data: chartData.countData,
    },
    {
      name: "Total Amount (K)",
      data: chartData.amountData,
    },
  ];

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Transaction Trends
        </h3>
      </div>

      <div id="transaction-trends-chart">
        <Chart options={options} series={series} type="line" height={310} />
      </div>
    </div>
  );
}
